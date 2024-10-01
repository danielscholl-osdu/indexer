package org.opengroup.osdu.indexer.schema.converter.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.springframework.stereotype.Component;

import static org.opengroup.osdu.indexer.config.IndexerConfigurationProperties.MAP_BOOL2STRING_FEATURE_NAME;

@Component
@ConfigurationProperties(prefix = "schema.converter")
@Getter
@Setter
public class SchemaConverterPropertiesConfig implements SchemaConverterConfig {

    private Set<String> skippedDefinitions;
    private Set<String> supportedArrayTypes;
    private Map<String, String> specialDefinitionsMap;
    private Map<String, String> primitiveTypesMap;
    private Set<String> processedArraysTypes;
    private String defaultObjectArraysType;

    @Autowired
    private IFeatureFlag featureFlagChecker;

    public SchemaConverterPropertiesConfig(IFeatureFlag flag) {
        if (flag != null) featureFlagChecker=flag;
        skippedDefinitions = getDefaultSkippedDefinitions();
        supportedArrayTypes = getDefaultSupportedArrayTypes();
        specialDefinitionsMap = getDefaultSpecialDefinitionsMap();
        primitiveTypesMap = getDefaultPrimitiveTypesMap();
        processedArraysTypes = getDefaultArraysTypesForProcessing();
        defaultObjectArraysType = getObjectArraysDefaultType();
    }

    public void resetToDefault() {
        skippedDefinitions = getDefaultSkippedDefinitions();
        supportedArrayTypes = getDefaultSupportedArrayTypes();
        specialDefinitionsMap = getDefaultSpecialDefinitionsMap();
        primitiveTypesMap = getDefaultPrimitiveTypesMap();
        processedArraysTypes = getDefaultArraysTypesForProcessing();
        defaultObjectArraysType = getObjectArraysDefaultType();
    }

    private Set<String> getDefaultSkippedDefinitions() {
        return new HashSet<>(Arrays.asList("AbstractAnyCrsFeatureCollection",
            "anyCrsGeoJsonFeatureCollection"));
    }

    private Set<String> getDefaultSupportedArrayTypes() {
        return new HashSet<>(Arrays.asList("boolean", "integer", "number", "string", "object"));
    }

    private Map<String, String> getDefaultSpecialDefinitionsMap() {
        Map<String, String> defaultSpecialDefinitions = new HashMap<>();

        defaultSpecialDefinitions.put("AbstractFeatureCollection", "core:dl:geoshape");
        defaultSpecialDefinitions.put("core_dl_geopoint", "core:dl:geopoint");
        defaultSpecialDefinitions.put("geoJsonFeatureCollection", "core:dl:geoshape");

        return defaultSpecialDefinitions;
    }

    private Map<String, String> getDefaultPrimitiveTypesMap() {
        Map<String, String> defaultPrimitiveTypesMap = new HashMap<>();

        if (this.featureFlagChecker.isFeatureEnabled(MAP_BOOL2STRING_FEATURE_NAME)) {
            // in the earlier versions boolean was translated to bool and
            // this caused mapping boolean values like text as entry in StorageType entry in map is boolean
            // in some places boolean is still presented as bool so here both are normalized to boolean
            defaultPrimitiveTypesMap.put("boolean", "boolean");
            defaultPrimitiveTypesMap.put("bool", "boolean");
        } else {
            defaultPrimitiveTypesMap.put("boolean", "bool");
        }

        defaultPrimitiveTypesMap.put("number", "double");
        defaultPrimitiveTypesMap.put("date-time", "datetime");
        defaultPrimitiveTypesMap.put("date", "datetime");
        defaultPrimitiveTypesMap.put("time", "datetime");
        defaultPrimitiveTypesMap.put("int32", "int");
        defaultPrimitiveTypesMap.put("integer", "int");
        defaultPrimitiveTypesMap.put("int64", "long");

        return defaultPrimitiveTypesMap;
    }

    private Set<String> getDefaultArraysTypesForProcessing() {
        return new HashSet<>(Arrays.asList("nested"));
    }

    private String getObjectArraysDefaultType() {
        return "[]object";
    }
}
