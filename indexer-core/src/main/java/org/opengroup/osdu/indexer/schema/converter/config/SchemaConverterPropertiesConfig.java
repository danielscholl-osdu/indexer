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
import org.opengroup.osdu.indexer.util.BooleanFeatureFlagClient;
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
    @Autowired
    private BooleanFeatureFlagClient partitionFlagChecker;

    public SchemaConverterPropertiesConfig(IFeatureFlag flag, BooleanFeatureFlagClient flagClient) {
        if (flag != null) featureFlagChecker=flag;
        if (flagClient != null) partitionFlagChecker=flagClient;
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

        /*
            Logic behing feature flag settings
            If service-level FF is ON for a given deployment/environment, then the FF is ON for all the data partitions under the deployment/environment
            If service-level FF is OFF but it is ON for a given data partition, then the FF is ON for the given data partition

            With this solution, the service providers can decide which level of FF should be turned on. For example,
            If there are only few data partitions in a given deployment and there are not many data needed to be indexed, 
            the service provider can turn on the service-level FF and re-index all the data partitions in one shot.
            If there are lots of data partitions or some data partitions have lots of data in a given deployment, step should be as following.

            1. Turn the service-level FF to be OFF
            2. Turn on the FF via partition service for a given data partition and re-index all the data in the given data partition
            3. Repeat step 2 until all the data partitions have been re-indexed
            4. Turn the service-level FF to be ON and re-deploy the service. So all new data partitions apply the fix
         */
        if (this.featureFlagChecker.isFeatureEnabled(MAP_BOOL2STRING_FEATURE_NAME) || partitionFlagChecker.isEnabled(MAP_BOOL2STRING_FEATURE_NAME, false)) {
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
