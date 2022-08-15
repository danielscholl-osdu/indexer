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

package org.opengroup.osdu.indexer.util;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.entitlements.AclRole;
import org.opengroup.osdu.core.common.model.indexer.ElasticType;
import org.opengroup.osdu.core.common.model.indexer.Records;
import org.opengroup.osdu.core.common.model.indexer.StorageType;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeMapper {

    private static final Map<String, String> storageToIndexerType = new HashMap<>();

    private static final Map<String, Object> metaAttributeIndexerType = new HashMap<>();

    private static final String STORAGE_TYPE_OBJECTS = "[]object";

    private static final String STORAGE_TYPE_NESTED = "nested";

    private static final String STORAGE_TYPE_FLATTENED = "flattened";

    static {

        metaAttributeIndexerType.put(RecordMetaAttribute.KIND.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.TYPE.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.ID.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.NAMESPACE.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.VERSION.getValue(), ElasticType.LONG.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.X_ACL.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.ACL.getValue(), getAclIndexerMapping());
        metaAttributeIndexerType.put(RecordMetaAttribute.TAGS.getValue(), ElasticType.FLATTENED.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.LEGAL.getValue(), getLegalIndexerMapping());
        metaAttributeIndexerType.put(RecordMetaAttribute.ANCESTRY.getValue(), getAncestryIndexerMapping());
        metaAttributeIndexerType.put(RecordMetaAttribute.INDEX_STATUS.getValue(), getIndexStatusMapping());
        metaAttributeIndexerType.put(RecordMetaAttribute.AUTHORITY.getValue(), getConstantKeywordMap());
        metaAttributeIndexerType.put(RecordMetaAttribute.SOURCE.getValue(), getConstantKeywordMap());
        metaAttributeIndexerType.put(RecordMetaAttribute.CREATE_USER.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.MODIFY_USER.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.CREATE_TIME.getValue(), ElasticType.DATE.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.MODIFY_TIME.getValue(), ElasticType.DATE.getValue());

        storageToIndexerType.put(StorageType.LINK.getValue(), ElasticType.KEYWORD.getValue());
        storageToIndexerType.put(StorageType.LINK_ARRAY.getValue(), ElasticType.KEYWORD_ARRAY.getValue());
        storageToIndexerType.put(StorageType.BOOLEAN.getValue(), ElasticType.BOOLEAN.getValue());
        storageToIndexerType.put(StorageType.BOOLEAN_ARRAY.getValue(), ElasticType.BOOLEAN_ARRAY.getValue());
        storageToIndexerType.put(StorageType.STRING.getValue(), ElasticType.TEXT.getValue());
        storageToIndexerType.put(StorageType.STRING_ARRAY.getValue(), ElasticType.TEXT_ARRAY.getValue());
        storageToIndexerType.put(StorageType.INT.getValue(), ElasticType.INTEGER.getValue());
        storageToIndexerType.put(StorageType.INT_ARRAY.getValue(), ElasticType.INTEGER_ARRAY.getValue());
        storageToIndexerType.put(StorageType.FLOAT.getValue(), ElasticType.FLOAT.getValue());
        storageToIndexerType.put(StorageType.FLOAT_ARRAY.getValue(), ElasticType.FLOAT_ARRAY.getValue());
        storageToIndexerType.put(StorageType.DOUBLE.getValue(), ElasticType.DOUBLE.getValue());
        storageToIndexerType.put(StorageType.DOUBLE_ARRAY.getValue(), ElasticType.DOUBLE_ARRAY.getValue());
        storageToIndexerType.put(StorageType.LONG.getValue(), ElasticType.LONG.getValue());
        storageToIndexerType.put(StorageType.LONG_ARRAY.getValue(), ElasticType.LONG_ARRAY.getValue());
        storageToIndexerType.put(StorageType.DATETIME.getValue(), ElasticType.DATE.getValue());
        storageToIndexerType.put(StorageType.DATETIME_ARRAY.getValue(), ElasticType.DATE_ARRAY.getValue());
        storageToIndexerType.put(StorageType.GEO_POINT.getValue(), ElasticType.GEO_POINT.getValue());
        storageToIndexerType.put(StorageType.GEO_SHAPE.getValue(), ElasticType.GEO_SHAPE.getValue());

        //TODO temporary fix for https://community.opengroup.org/osdu/platform/system/indexer-service/-/issues/1
        storageToIndexerType.put(STORAGE_TYPE_OBJECTS, ElasticType.OBJECT.getValue());
        storageToIndexerType.put(STORAGE_TYPE_NESTED, ElasticType.NESTED.getValue());
        storageToIndexerType.put(STORAGE_TYPE_FLATTENED, ElasticType.FLATTENED.getValue());
    }

    public static String getIndexerType(String storageType, String defaultType) {
        return storageToIndexerType.getOrDefault(storageType, defaultType);
    }

    public static Object getIndexerType(RecordMetaAttribute attribute) {
        return metaAttributeIndexerType.getOrDefault(attribute.getValue(), null);
    }

    private static Object getConstantIndexerType(String key, String value) {
        Map<String, Object> constantAttribute = (Map<String, Object>) metaAttributeIndexerType.get(key);
        constantAttribute.put("value", value);
        return constantAttribute;
    }

    public static List<String> getMetaAttributesKeys() {
        return new ArrayList<>(metaAttributeIndexerType.keySet());
    }

    public static Object getMetaAttributeIndexerMapping(String key, String value) {
        if (key.equals(RecordMetaAttribute.ACL.getValue())
                || key.equals(RecordMetaAttribute.LEGAL.getValue()) || key.equals(RecordMetaAttribute.ANCESTRY.getValue()) || key.equals(RecordMetaAttribute.INDEX_STATUS.getValue())) {
            return metaAttributeIndexerType.get(key);
        } else if (key.equals(RecordMetaAttribute.AUTHORITY.getValue()) || key.equals(RecordMetaAttribute.SOURCE.getValue())) {
            return getConstantIndexerType(key, value);
        }
        return Records.Type.builder().type(metaAttributeIndexerType.get(key).toString()).build();
    }

    public static Object getDataAttributeIndexerMapping(Object indexerType) {
        if (ElasticType.TEXT.getValue().equalsIgnoreCase(indexerType.toString())) {
            return getTextIndexerMapping();
        }

        if (isArray(indexerType.toString())) {
            String memberType = getArrayMemberType(indexerType.toString());
            if (ElasticType.TEXT.getValue().equalsIgnoreCase(memberType)) {
                return getTextIndexerMapping();
            }
            return Records.Type.builder().type(memberType).build();
        }

        if (isMap(indexerType)) {
            Map<String, Object> type = (Map<String, Object>) indexerType;
            Map<String, Object> propertiesMap = (Map<String, Object>) type.get(Constants.PROPERTIES);
            for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
                if (isMap(entry.getValue())) {
                    entry.setValue(getDataAttributeIndexerMapping(entry.getValue()));
                } else if (ElasticType.TEXT.getValue().equalsIgnoreCase(String.valueOf(entry.getValue()))) {
                    entry.setValue(getTextIndexerMapping());
                } else if (isArray(String.valueOf(entry.getValue()))) {
                    entry.setValue(Records.Type.builder().type(getArrayMemberType(String.valueOf(entry.getValue()))).build());
                } else {
                    entry.setValue(Records.Type.builder().type(entry.getValue().toString()).build());
                }
            }
            return indexerType;
        }

        return Records.Type.builder().type(indexerType.toString()).build();
    }

    private static boolean isMap(Object indexerType) {
        return indexerType instanceof Map;
    }

    private static boolean isArray(String indexerType) {
        return indexerType != null && indexerType.endsWith("_array");
    }

    private static String getArrayMemberType(String indexerType) {
        return StringUtils.substringBefore(indexerType, "_");
    }

    private static Object getAclIndexerMapping() {
        Map<String, Object> aclRoleMapping = new HashMap<>();
        aclRoleMapping.put(AclRole.VIEWERS.getValue(), Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());
        aclRoleMapping.put(AclRole.OWNERS.getValue(), Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());

        Map<String, Object> aclProperties = new HashMap<>();
        aclProperties.put(Constants.PROPERTIES, aclRoleMapping);

        return aclProperties;
    }

    private static Object getLegalIndexerMapping() {
        Map<String, Object> legalComplianceMapping = new HashMap<>();
        legalComplianceMapping.put("legaltags", Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());
        legalComplianceMapping.put("otherRelevantDataCountries", Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());
        legalComplianceMapping.put("status", Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());

        Map<String, Object> legalProperties = new HashMap<>();
        legalProperties.put(Constants.PROPERTIES, legalComplianceMapping);

        return legalProperties;
    }

    private static Object getAncestryIndexerMapping() {
        Map<String, Object> ancestryMapping = new HashMap<>();
        ancestryMapping.put("parents", Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());

        Map<String, Object> ancestryProperties = new HashMap<>();
        ancestryProperties.put(Constants.PROPERTIES, ancestryMapping);

        return ancestryProperties;
    }

    public static Object getObjectsArrayMapping(String dataType, Object properties) {
        Map<String, Object> nestedMapping = new HashMap<>();
        nestedMapping.put(Constants.TYPE, storageToIndexerType.getOrDefault(dataType, dataType));
        nestedMapping.put(Constants.PROPERTIES, properties);
        return nestedMapping;
    }

    private static Object getIndexStatusMapping() {
        Map<String, Object> indexStatusMapping = new HashMap<>();
        indexStatusMapping.put("statusCode", Records.Type.builder().type(ElasticType.INTEGER.getValue()).build());
        indexStatusMapping.put("trace", Records.Type.builder().type(ElasticType.TEXT.getValue()).build());
        indexStatusMapping.put("lastUpdateTime", Records.Type.builder().type(ElasticType.DATE.getValue()).build());

        Map<String, Object> indexStatusProperties = new HashMap<>();
        indexStatusProperties.put(Constants.PROPERTIES, indexStatusMapping);

        return indexStatusProperties;
    }

    private static Object getTextIndexerMapping() {
        Map<String, Object> fieldIndexTypeMap = getKeywordMap();
        Map<String, Object> textMap = new HashMap<>();
        textMap.put("type", "text");
        textMap.put("fields", fieldIndexTypeMap);
        return textMap;
    }

    private static Map<String, Object> getKeywordMap() {
        Map<String, Object> keywordMap = new HashMap<>();
        keywordMap.put("type", "keyword");
        keywordMap.put("ignore_above", 256);
        keywordMap.put("null_value", "null");
        Map<String, Object> fieldIndexTypeMap = new HashMap<>();
        fieldIndexTypeMap.put("keyword", keywordMap);
        return fieldIndexTypeMap;
    }

    private static Map<String, Object> getConstantKeywordMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "constant_keyword");
        return map;
    }
}