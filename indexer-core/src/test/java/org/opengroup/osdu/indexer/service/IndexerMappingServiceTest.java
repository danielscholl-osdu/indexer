// Copyright © Schlumberger
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

import com.google.gson.Gson;
import org.apache.http.StatusLine;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.Invocation;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.indexer.cache.partitionsafe.IndexCache;
import org.opengroup.osdu.indexer.service.exception.ElasticsearchMappingException;
import org.opengroup.osdu.indexer.util.ElasticClientHandler;
import org.opengroup.osdu.indexer.util.TypeMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opengroup.osdu.indexer.config.IndexerConfigurationProperties.BAG_OF_WORDS_FEATURE_NAME;
import static org.opengroup.osdu.indexer.config.IndexerConfigurationProperties.KEYWORD_LOWER_FEATURE_NAME;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {IFeatureFlag.class})
public class IndexerMappingServiceTest {

    private final String kind = "tenant:test:test:1.0.0";
    private final String index = "tenant-test-test-1.0.0";
    private final String type = "test";
    private final String validMapping = "{\"dynamic\":false,\"properties\":{\"data\":{\"properties\":{\"Msg\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"null_value\":\"null\",\"ignore_above\":256,\"type\":\"keyword\"}}},\"Intervals\":{\"properties\":{\"StopMarkerID\":{\"type\":\"keyword\"},\"GeologicUnitInterpretationIDs\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"null_value\":\"null\",\"ignore_above\":256,\"type\":\"keyword\"}}},\"StopMeasuredDepth\":{\"type\":\"double\"}}},\"Location\":{\"type\":\"geo_point\"}}},\"bagOfWords\":{\"store\":true,\"type\":\"text\",\"fields\":{\"autocomplete\":{\"type\":\"completion\"}}},\"authority\":{\"type\":\"constant_keyword\",\"value\":\"tenant\"},\"id\":{\"type\":\"keyword\"},\"acl\":{\"properties\":{\"viewers\":{\"type\":\"keyword\"},\"owners\":{\"type\":\"keyword\"}}}}}";
    private final String validKeywordLowerMapping = "{\"dynamic\":false,\"properties\":{\"data\":{\"properties\":{\"Msg\":{\"type\":\"text\",\"fields\":{\"keywordLower\":{\"normalizer\":\"lowercase\",\"null_value\":\"null\",\"ignore_above\":256,\"type\":\"keyword\"},\"keyword\":{\"null_value\":\"null\",\"ignore_above\":256,\"type\":\"keyword\"}}},\"Intervals\":{\"properties\":{\"StopMarkerID\":{\"type\":\"keyword\"},\"GeologicUnitInterpretationIDs\":{\"type\":\"text\",\"fields\":{\"keywordLower\":{\"normalizer\":\"lowercase\",\"null_value\":\"null\",\"ignore_above\":256,\"type\":\"keyword\"},\"keyword\":{\"null_value\":\"null\",\"ignore_above\":256,\"type\":\"keyword\"}}},\"StopMeasuredDepth\":{\"type\":\"double\"}}},\"Location\":{\"type\":\"geo_point\"}}},\"bagOfWords\":{\"store\":true,\"type\":\"text\",\"fields\":{\"autocomplete\":{\"type\":\"completion\"}}},\"authority\":{\"type\":\"constant_keyword\",\"value\":\"tenant\"},\"id\":{\"type\":\"keyword\"},\"acl\":{\"properties\":{\"viewers\":{\"type\":\"keyword\"},\"owners\":{\"type\":\"keyword\"}}}}}";
    private final String emptyDataValidMapping = "{\"dynamic\":false,\"properties\":{\"id\":{\"type\":\"keyword\"},\"acl\":{\"properties\":{\"viewers\":{\"type\":\"keyword\"},\"owners\":{\"type\":\"keyword\"}}},\"bagOfWords\":{\"store\":true,\"type\":\"text\",\"fields\":{\"autocomplete\":{\"type\":\"completion\"}}},\"authority\":{\"type\":\"constant_keyword\",\"value\":\"tenant\"}}}";

    @Mock
    private RestClient restClient;
    @Mock
    private Response response;
    @Mock
    private StatusLine statusLine;
    @Mock
    private JaxRsDpsLog log;
    @Mock
    private ElasticClientHandler elasticClientHandler;
    @Mock
    private IndexCache indexCache;
    @Mock
    private IndicesService indicesService;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @MockBean
    private IFeatureFlag featureFlag;

    @Spy
    @InjectMocks
    private IndexerMappingServiceImpl sut = new IndexerMappingServiceImpl();

    private IndexSchema indexSchema;
    private IndexSchema noDataIndexSchema;

    private IndicesClient indicesClient;
    private RestHighLevelClient restHighLevelClient;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        this.indexSchema = IndexSchema.builder().kind(kind).type(type).dataSchema(getDataAttributeMapping()).metaSchema(getMetaAttributeMapping()).build();
        this.noDataIndexSchema = IndexSchema.builder().kind(kind).type(type).dataSchema(null).metaSchema(getMetaAttributeMapping()).build();

        this.indicesClient = mock(IndicesClient.class);
        this.restHighLevelClient = mock(RestHighLevelClient.class);

        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn(index);
        when(this.restHighLevelClient.getLowLevelClient()).thenReturn(restClient);
        when(this.restClient.performRequest(any())).thenReturn(response);
        when(this.response.getStatusLine()).thenReturn(statusLine);
        when(this.statusLine.getStatusCode()).thenReturn(200);
    }

    private Map<String, Object> getMetaAttributeMapping() {
        Map<String, Object> metaMapping = new HashMap<>();
        metaMapping.put(RecordMetaAttribute.ID.getValue(), "keyword");
        metaMapping.put(RecordMetaAttribute.ACL.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.ACL));
        metaMapping.put(RecordMetaAttribute.BAG_OF_WORDS.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.BAG_OF_WORDS));
        metaMapping.put(RecordMetaAttribute.AUTHORITY.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.AUTHORITY));
        return metaMapping;
    }

    private Map<String, Object> getDataAttributeMapping() {
        Map<String, Object> dataMapping = new HashMap<>();
        dataMapping.put("Location", "geo_point");
        dataMapping.put("Msg", "text");
        Map<String, Object> intervalNestedAttribute = new HashMap<>();
        Map<String, Object> intervalProperties = new HashMap<>();
        intervalProperties.put("StopMarkerID", "link");
        intervalProperties.put("GeologicUnitInterpretationIDs", "text");
        intervalProperties.put("StopMeasuredDepth", "double");
        intervalNestedAttribute.put("properties", intervalProperties);
        dataMapping.put("Intervals", intervalNestedAttribute);
        return dataMapping;
    }

    @Test
    public void should_returnValidMapping_givenFalseMerge_keywordLowerDisabled_createMappingTest() {
        when(this.featureFlag.isFeatureEnabled(KEYWORD_LOWER_FEATURE_NAME)).thenReturn(false);
        try {
            String mapping = this.sut.createMapping(restHighLevelClient, indexSchema, index, false);
            assertEquals(validMapping, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnValidMapping_givenFalseMerge_keywordLowerEnabled_createMappingTest() {
        when(this.featureFlag.isFeatureEnabled(KEYWORD_LOWER_FEATURE_NAME)).thenReturn(true);
        try {
            String mapping = this.sut.createMapping(restHighLevelClient, indexSchema, index, false);
            assertEquals(validKeywordLowerMapping, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }


    @Test
    public void should_returnValidMapping_givenTrueMerge_createMappingTest() {
        try {
            AcknowledgedResponse mappingResponse = new AcknowledgedResponse(true);
            doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
            doReturn(mappingResponse).when(this.indicesClient).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));

            String mapping = this.sut.createMapping(this.restHighLevelClient, this.indexSchema, this.index, true);
            assertEquals(this.validMapping, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnValidMapping_givenTrueMerge_keywordLowerEnabled_createMappingTest() {
        when(this.featureFlag.isFeatureEnabled(KEYWORD_LOWER_FEATURE_NAME)).thenReturn(true);
        try {
            AcknowledgedResponse mappingResponse = new AcknowledgedResponse(true);
            doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
            doReturn(mappingResponse).when(this.indicesClient).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));

            String mapping = this.sut.createMapping(this.restHighLevelClient, this.indexSchema, this.index, true);
            assertEquals(validKeywordLowerMapping, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnValidMapping_givenExistType_createMappingTest() {
        try {
            AcknowledgedResponse mappingResponse = new AcknowledgedResponse(true);
            doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
            doReturn(mappingResponse).when(this.indicesClient).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));

            IndexerMappingServiceImpl indexerMappingServiceLocal = spy(new IndexerMappingServiceImpl());
            String mapping = this.sut.createMapping(this.restHighLevelClient, this.indexSchema, this.index, true);
            assertEquals(validMapping, mapping);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnDocumentMapping_givenValidIndexSchema() {
        try {
            Map<String, Object> documentMapping = this.sut.getIndexMappingFromRecordSchema(this.indexSchema);
            String documentMappingJson = new Gson().toJson(documentMapping);
            assertEquals(validMapping, documentMappingJson);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnDocumentMapping_givenValidEmptyDataIndexSchema() {
        try {
            IndexSchema emptyDataIndexSchema = IndexSchema.builder().kind(kind).type(type).metaSchema(getMetaAttributeMapping()).build();
            Map<String, Object> documentMapping = this.sut.getIndexMappingFromRecordSchema(emptyDataIndexSchema);
            String documentMappingJson = new Gson().toJson(documentMapping);
            assertEquals(emptyDataValidMapping, documentMappingJson);
        } catch (Exception e) {
            fail("Should not throw this exception" + e.getMessage());
        }
    }

    @Test
    public void should_returnCachedStatus_givenUpdatedIndex() throws Exception {
        final String cacheKey = String.format("metaAttributeMappingSynced-%s", index);
        when(this.indexCache.get(cacheKey)).thenReturn(true);

        this.sut.syncMetaAttributeIndexMappingIfRequired(restHighLevelClient, indexSchema);
        Collection<Invocation> invocations = mockingDetails(this.sut).getInvocations();
        assertEquals(1, invocations.size());
    }

    @Test
    public void should_applyNoUpdate_givenUpdateIndex() throws Exception {
        final String cacheKey = String.format("metaAttributeMappingSynced-%s", index);
        final String mapping = "{\"dynamic\":\"false\",\"properties\":{\"acl\":{\"properties\":{\"owners\":{\"type\":\"keyword\"},\"viewers\":{\"type\":\"keyword\"}}},\"ancestry\":{\"properties\":{\"parents\":{\"type\":\"keyword\"}}},\"authority\":{\"type\":\"constant_keyword\",\"value\":\"opendes\"},\"createTime\":{\"type\":\"date\"},\"createUser\":{\"type\":\"keyword\"},\"data\":{\"properties\":{\"message\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"null_value\":\"null\",\"ignore_above\":256}}}}},\"id\":{\"type\":\"keyword\"},\"index\":{\"properties\":{\"lastUpdateTime\":{\"type\":\"date\"},\"statusCode\":{\"type\":\"integer\"},\"trace\":{\"type\":\"text\"}}},\"kind\":{\"type\":\"keyword\"},\"legal\":{\"properties\":{\"legaltags\":{\"type\":\"keyword\"},\"otherRelevantDataCountries\":{\"type\":\"keyword\"},\"status\":{\"type\":\"keyword\"}}},\"modifyTime\":{\"type\":\"date\"},\"modifyUser\":{\"type\":\"keyword\"},\"namespace\":{\"type\":\"keyword\"},\"source\":{\"type\":\"constant_keyword\",\"value\":\"test\"},\"tags\":{\"type\":\"flattened\"},\"type\":{\"type\":\"keyword\"},\"version\":{\"type\":\"long\"},\"x-acl\":{\"type\":\"keyword\"},\"bagOfWords\":{\"search_analyzer\":\"whitespace\",\"analyzer\":\"detailExtractor\",\"store\":true,\"type\":\"text\",\"fields\":{\"autocomplete\":{\"type\":\"completion\",\"analyzer\":\"detailExtractor\",\"search_analyzer\":\"whitespace\",\"max_input_length\":256}}}}}";
        doReturn(mapping).when(this.sut).getIndexMapping(restHighLevelClient, index);
        this.sut.syncMetaAttributeIndexMappingIfRequired(restHighLevelClient, noDataIndexSchema);

        verify(this.indexCache, times(1)).get(cacheKey);
        verify(this.indexCache, times(1)).put(cacheKey, true);
    }

    @Test
    public void should_applyUpdate_givenExistingIndex() throws Exception {
        final String cacheKey = String.format("metaAttributeMappingSynced-%s", index);
        final String mapping = "{\"dynamic\":\"false\",\"properties\":{\"acl\":{\"properties\":{\"owners\":{\"type\":\"keyword\"},\"viewers\":{\"type\":\"keyword\"}}},\"ancestry\":{\"properties\":{\"parents\":{\"type\":\"keyword\"}}},\"data\":{\"properties\":{\"message\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"null_value\":\"null\",\"ignore_above\":256}}}}},\"id\":{\"type\":\"keyword\"},\"index\":{\"properties\":{\"lastUpdateTime\":{\"type\":\"date\"},\"statusCode\":{\"type\":\"integer\"},\"trace\":{\"type\":\"text\"}}},\"kind\":{\"type\":\"keyword\"},\"legal\":{\"properties\":{\"legaltags\":{\"type\":\"keyword\"},\"otherRelevantDataCountries\":{\"type\":\"keyword\"},\"status\":{\"type\":\"keyword\"}}},\"namespace\":{\"type\":\"keyword\"},\"tags\":{\"type\":\"flattened\"},\"type\":{\"type\":\"keyword\"},\"version\":{\"type\":\"long\"},\"x-acl\":{\"type\":\"keyword\"}}}";
        doReturn(mapping).when(this.sut).getIndexMapping(restHighLevelClient, index);

        AcknowledgedResponse mappingResponse = new AcknowledgedResponse(true);
        doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
        doReturn(mappingResponse).when(this.indicesClient).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));

        this.sut.syncMetaAttributeIndexMappingIfRequired(restHighLevelClient, indexSchema);

        verify(this.indexCache, times(1)).get(cacheKey);
        verify(this.indexCache, times(1)).put(cacheKey, true);
        verify(this.indicesClient, times(1)).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));
    }

    @Test
    public void should_applyNoUpdate_onDataChange_givenMetaUpdate_onIndex() throws Exception {
        final String cacheKey = String.format("metaAttributeMappingSynced-%s", index);
        final String mapping = "{\"dynamic\":\"false\",\"properties\":{\"acl\":{\"properties\":{\"owners\":{\"type\":\"keyword\"},\"viewers\":{\"type\":\"keyword\"}}},\"ancestry\":{\"properties\":{\"parents\":{\"type\":\"keyword\"}}},\"authority\":{\"type\":\"constant_keyword\",\"value\":\"opendes\"},\"createTime\":{\"type\":\"date\"},\"createUser\":{\"type\":\"keyword\"},\"data\":{\"properties\":{\"message\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"null_value\":\"null\",\"ignore_above\":256}}}}},\"id\":{\"type\":\"keyword\"},\"index\":{\"properties\":{\"lastUpdateTime\":{\"type\":\"date\"},\"statusCode\":{\"type\":\"integer\"},\"trace\":{\"type\":\"text\"}}},\"kind\":{\"type\":\"keyword\"},\"legal\":{\"properties\":{\"legaltags\":{\"type\":\"keyword\"},\"otherRelevantDataCountries\":{\"type\":\"keyword\"},\"status\":{\"type\":\"keyword\"}}},\"modifyTime\":{\"type\":\"date\"},\"modifyUser\":{\"type\":\"keyword\"},\"namespace\":{\"type\":\"keyword\"},\"source\":{\"type\":\"constant_keyword\",\"value\":\"test\"},\"tags\":{\"type\":\"flattened\"},\"type\":{\"type\":\"keyword\"},\"version\":{\"type\":\"long\"},\"x-acl\":{\"type\":\"keyword\"},\"bagOfWords\":{\"search_analyzer\":\"whitespace\",\"analyzer\":\"detailExtractor\",\"store\":true,\"type\":\"text\",\"fields\":{\"autocomplete\":{\"type\":\"completion\",\"analyzer\":\"detailExtractor\",\"search_analyzer\":\"whitespace\",\"max_input_length\":256}}}}}";
        doReturn(mapping).when(this.sut).getIndexMapping(restHighLevelClient, index);
        when(this.featureFlag.isFeatureEnabled(BAG_OF_WORDS_FEATURE_NAME)).thenReturn(false);
        this.sut.syncMetaAttributeIndexMappingIfRequired(restHighLevelClient, noDataIndexSchema);

        verify(this.indexCache, times(1)).get(cacheKey);
        verify(this.indexCache, times(1)).put(cacheKey, true);
        verify(this.featureFlag, times(1)).isFeatureEnabled(BAG_OF_WORDS_FEATURE_NAME);
        try (MockedStatic<TypeMapper> theMock = Mockito.mockStatic(TypeMapper.class)) {
            theMock.verifyNoInteractions();
        }
    }

    @Test
    public void should_throwMappingException_givenElasticsearchException() throws Exception {
        String mapping = "{\"dynamic\":\"false\",\"properties\":{\"acl\":{\"properties\":{\"owners\":{\"type\":\"keyword\"},\"viewers\":{\"type\":\"keyword\"}}},\"ancestry\":{\"properties\":{\"parents\":{\"type\":\"keyword\"}}},\"data\":{\"properties\":{\"message\":{\"type\":\"text\",\"fields\":{\"keyword\":{\"type\":\"keyword\",\"null_value\":\"null\",\"ignore_above\":256}}}}},\"id\":{\"type\":\"keyword\"},\"index\":{\"properties\":{\"lastUpdateTime\":{\"type\":\"date\"},\"statusCode\":{\"type\":\"integer\"},\"trace\":{\"type\":\"text\"}}},\"kind\":{\"type\":\"keyword\"},\"legal\":{\"properties\":{\"legaltags\":{\"type\":\"keyword\"},\"otherRelevantDataCountries\":{\"type\":\"keyword\"},\"status\":{\"type\":\"keyword\"}}},\"namespace\":{\"type\":\"keyword\"},\"tags\":{\"type\":\"flattened\"},\"type\":{\"type\":\"keyword\"},\"version\":{\"type\":\"long\"},\"x-acl\":{\"type\":\"keyword\"}}}";
        String message = "testExceptionMessage";

        doReturn(mapping).when(this.sut).getIndexMapping(restHighLevelClient, index);
        doReturn(this.indicesClient).when(this.restHighLevelClient).indices();
        doThrow(new ElasticsearchException(message)).when(this.indicesClient).putMapping(any(PutMappingRequest.class), any(RequestOptions.class));

        ElasticsearchMappingException exception = assertThrows(
                ElasticsearchMappingException.class,
                () -> this.sut.syncMetaAttributeIndexMappingIfRequired(restHighLevelClient, indexSchema));

        assertEquals(message, exception.getMessage());
    }
}
