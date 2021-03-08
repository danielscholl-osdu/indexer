// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.service;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.RestHighLevelClient;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestStatus;
import org.opengroup.osdu.core.common.model.indexer.ElasticType;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.indexer.StorageType;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.core.common.search.IndicesService;
import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.opengroup.osdu.indexer.util.TypeMapper;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Service
public class IndexSchemaServiceImpl implements IndexSchemaService {

    private static final String FLATTENED_SCHEMA = "_flattened";
    private static final String WELLBORE_MARKER_SET = "WellboreMarkerSet";
    private static final String MARKERS = "Markers";
    private static final String WELL_LOG = "WellLog";
    private static final String CURVES = "Curves";

    private final Gson gson = new Gson();

    @Inject
    private JaxRsDpsLog log;
    @Inject
    private SchemaService schemaProvider;
    @Inject
    private ElasticClientHandler elasticClientHandler;
    @Inject
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Inject
    private IndexerMappingService mappingService;
    @Inject
    private IndicesService indicesService;
    @Inject
    private ISchemaCache schemaCache;

    public void processSchemaMessages(Map<String, OperationType> schemaMsgs) throws IOException {
        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            schemaMsgs.entrySet().forEach(msg -> {
                try {
                    processSchemaEvents(restClient, msg);
                } catch (IOException | ElasticsearchStatusException e) {
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "unable to process schema update", e.getMessage());
                }
            });
        }
    }

    private void processSchemaEvents(RestHighLevelClient restClient, Map.Entry<String, OperationType> msg) throws IOException, ElasticsearchStatusException {
        String kind = msg.getKey();
        String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);

        boolean indexExist = this.indicesService.isIndexExist(restClient, index);

        if (msg.getValue() == OperationType.create_schema) {
            // reset cache and get new schema
            this.invalidateCache(kind);
            IndexSchema schemaObj = this.getIndexerInputSchema(kind, true);
            if (schemaObj.isDataSchemaMissing()) {
                log.warning(String.format("schema not found for kind: %s", kind));
                return;
            }

            if (indexExist) {
                try {
                    // merge the mapping
                    this.mappingService.createMapping(restClient, schemaObj, index, true);
                } catch (AppException e) {
                    // acknowledge for TaskQueue and not retry
                    if (e.getError().getCode() == HttpStatus.SC_BAD_REQUEST) {
                        throw new AppException(RequestStatus.SCHEMA_CONFLICT, e.getError().getReason(), "error creating or merging index mapping");
                    }
                    throw e;
                }
            } else {
                // create index with mapping
                Map<String, Object> mapping = this.mappingService.getIndexMappingFromRecordSchema(schemaObj);
                this.indicesService.createIndex(restClient, index, null, schemaObj.getType(), mapping);
            }
        } else if (msg.getValue() == OperationType.purge_schema) {
            if (indexExist) {
                // reset schema cache
                this.invalidateCache(kind);
            } else {
                // log warning
                log.warning(String.format("Kind: %s not found", kind));
            }
        }
    }

    @Override
    public IndexSchema getIndexerInputSchema(String kind, boolean invalidateCached) throws AppException {

        if (invalidateCached) {
            this.invalidateCache(kind);
        }

        try {
            String schema = (String) this.schemaCache.get(kind);
            if (Strings.isNullOrEmpty(schema)) {
                // get from storage
                schema = getSchema(kind);
                if (Strings.isNullOrEmpty(schema)) {
                    Schema basicSchema = Schema.builder().kind(kind).build();
                    return normalizeSchema(gson.toJson(basicSchema));
                } else {
                    // cache the schema
                    this.schemaCache.put(kind, schema);
                    // get flatten schema and cache it
                    IndexSchema flatSchemaObj = normalizeSchema(schema);
                    if (flatSchemaObj != null) {
                        this.schemaCache.put(kind + FLATTENED_SCHEMA, gson.toJson(flatSchemaObj));
                    }
                    return flatSchemaObj;
                }
            } else {
                // search flattened schema in memcache
                String flattenedSchema = (String) this.schemaCache.get(kind + FLATTENED_SCHEMA);
                if (Strings.isNullOrEmpty(flattenedSchema)) {
                    Schema basicSchema = Schema.builder().kind(kind).build();
                    return normalizeSchema(gson.toJson(basicSchema));
                }
                return this.gson.fromJson(flattenedSchema, IndexSchema.class);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Schema parse/read error", "Error while reading schema via storage service.", e);
        }
    }

    private String getSchema(String kind) throws URISyntaxException, UnsupportedEncodingException {
        return this.schemaProvider.getSchema(kind);
    }

    public void syncIndexMappingWithStorageSchema(String kind) throws ElasticsearchException, IOException, AppException {
        String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            if (this.indicesService.isIndexExist(restClient, index)) {
                this.indicesService.deleteIndex(restClient, index);
                this.log.info(String.format("deleted index: %s", index));
            }
            IndexSchema schemaObj = this.getIndexerInputSchema(kind, true);
            this.indicesService.createIndex(restClient, index, null, schemaObj.getType(), this.mappingService.getIndexMappingFromRecordSchema(schemaObj));
        }
    }

    public boolean isStorageSchemaSyncRequired(String kind, boolean forceClean) throws IOException {
        String index = this.elasticIndexNameResolver.getIndexNameFromKind(kind);
        try (RestHighLevelClient restClient = this.elasticClientHandler.createRestClient()) {
            boolean indexExist = this.indicesService.isIndexExist(restClient, index);
            return !indexExist || forceClean;
        }
    }

    private void invalidateCache(String kind) {
        String schema = (String) this.schemaCache.get(kind);
        if (!Strings.isNullOrEmpty(schema)) this.schemaCache.delete(kind);

        String flattenSchema = (String) this.schemaCache.get(kind + FLATTENED_SCHEMA);
        if (!Strings.isNullOrEmpty(flattenSchema)) this.schemaCache.delete(kind + FLATTENED_SCHEMA);
    }

    private IndexSchema normalizeSchema(String schemaStr) throws AppException {

        try {
            Schema schemaObj = this.gson.fromJson(schemaStr, Schema.class);

            if (schemaObj == null) return null;

            Map<String, String> data = new HashMap<>();
            Map<String, Object> meta = new HashMap<>();

            if (schemaObj.getSchema() != null && schemaObj.getSchema().length > 0) {
                for (SchemaItem schemaItem : schemaObj.getSchema()) {
                    String dataType = schemaItem.getKind();
                    String elasticDataType = TypeMapper.getIndexerType(dataType);
                    if (elasticDataType == null) {
                        elasticDataType = TypeMapper.getIndexerType(StorageType.STRING.getValue());
                    }
                    data.put(schemaItem.getPath(), elasticDataType);
                }
            }

            // mandatory attributes
            meta.put(RecordMetaAttribute.ID.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.ID));
            meta.put(RecordMetaAttribute.NAMESPACE.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.NAMESPACE));
            meta.put(RecordMetaAttribute.VERSION.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.VERSION));
            meta.put(RecordMetaAttribute.KIND.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.KIND));
            meta.put(RecordMetaAttribute.TYPE.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.TYPE));
            meta.put(RecordMetaAttribute.ACL.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.ACL));
            meta.put(RecordMetaAttribute.X_ACL.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.X_ACL));
            meta.put(RecordMetaAttribute.TAGS.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.TAGS));
            meta.put(RecordMetaAttribute.LEGAL.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.LEGAL));
            meta.put(RecordMetaAttribute.ANCESTRY.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.ANCESTRY));
            meta.put(RecordMetaAttribute.INDEX_STATUS.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.INDEX_STATUS));

            String kind = schemaObj.getKind();
            String type = kind.split(":")[2];

            //TODO temporary fix for https://community.opengroup.org/osdu/platform/system/indexer-service/-/issues/1
            if(data.get(MARKERS) != null){
                data.put(MARKERS, ElasticType.NESTED.getValue());
            }
            if(data.get(CURVES) != null){
                data.put(CURVES, ElasticType.NESTED.getValue());
            }

            return IndexSchema.builder().dataSchema(data).metaSchema(meta).kind(kind).type(type).build();

        } catch (Exception e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Schema normalization error", "An error has occurred while normalizing the schema.", e);
        }
    }

}