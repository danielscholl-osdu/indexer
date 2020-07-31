package org.opengroup.osdu.util;

public class Config {

    private static final String DEFAULT_ELASTIC_HOST = "";
    private static final String DEFAULT_ELASTIC_USER_NAME = "";
    private static final String DEFAULT_ELASTIC_PASSWORD = "";
    private static final int PORT = 9243;
    private static final String DEFAULT_ELASTIC_SSL_ENABLED = "true";

    private static final String DEFAULT_INDEXER_HOST = "";
    private static final String DEFAULT_SEARCH_HOST = "";
    private static final String DEFAULT_STORAGE_HOST = "";
    private static final String DEFAULT_DATA_PARTITION_ID_TENANT1 = "";
    private static final String DEFAULT_DATA_PARTITION_ID_TENANT2 = "";
    private static final String DEFAULT_SEARCH_INTEGRATION_TESTER = "";

    private static final String DEFAULT_TARGET_AUDIENCE = "";

    private static final String DEFAULT_LEGAL_TAG = "";
    private static final String DEFAULT_OTHER_RELEVANT_DATA_COUNTRIES = "";

    private static final String DEFAULT_ENTITLEMENTS_DOMAIN = "";


    public static int getPort() {
        return Integer.parseInt(getEnvironmentVariableOrDefaultValue("ELASTIC_PORT", String.valueOf(PORT)));
    }

    public static String getOtherRelevantDataCountries() {
        return getEnvironmentVariableOrDefaultValue("OTHER_RELEVANT_DATA_COUNTRIES", DEFAULT_OTHER_RELEVANT_DATA_COUNTRIES);
    }

    public static String getLegalTag() {
        return getEnvironmentVariableOrDefaultValue("LEGAL_TAG", DEFAULT_LEGAL_TAG);
    }

    public static String getTargetAudience() {
        return getEnvironmentVariableOrDefaultValue("INTEGRATION_TEST_AUDIENCE", DEFAULT_TARGET_AUDIENCE);
    }

    public static String getKeyValue() {
        return getEnvironmentVariableOrDefaultValue("SEARCH_INTEGRATION_TESTER", DEFAULT_SEARCH_INTEGRATION_TESTER);
    }

    public static String getDataPartitionIdTenant1() {
        return getEnvironmentVariableOrDefaultValue("DEFAULT_DATA_PARTITION_ID_TENANT1", DEFAULT_DATA_PARTITION_ID_TENANT1);
    }

    public static String getDataPartitionIdTenant2() {
        return getEnvironmentVariableOrDefaultValue("DEFAULT_DATA_PARTITION_ID_TENANT2", DEFAULT_DATA_PARTITION_ID_TENANT2);
    }

    public static String getUserName() {
        return getEnvironmentVariableOrDefaultValue("ELASTIC_USER_NAME", DEFAULT_ELASTIC_USER_NAME);
    }

    public static String getPassword() {
        return getEnvironmentVariableOrDefaultValue("ELASTIC_PASSWORD", DEFAULT_ELASTIC_PASSWORD);
    }

    public static String getElasticHost() {
        return getEnvironmentVariableOrDefaultValue("ELASTIC_HOST", DEFAULT_ELASTIC_HOST);
    }

    public static boolean isElasticSslEnabled() {
        return Boolean.parseBoolean(getEnvironmentVariableOrDefaultValue("ELASTIC_SSL_ENABLED", DEFAULT_ELASTIC_SSL_ENABLED));
    }

    public static String getIndexerBaseURL() {
        return getEnvironmentVariableOrDefaultValue("INDEXER_HOST", DEFAULT_INDEXER_HOST);
    }

    public static String getSearchBaseURL() {
        return getEnvironmentVariableOrDefaultValue("SEARCH_HOST", DEFAULT_SEARCH_HOST);
    }

    public static String getStorageBaseURL() {
        return getEnvironmentVariableOrDefaultValue("STORAGE_HOST", DEFAULT_STORAGE_HOST);
    }

    public static String getEntitlementsDomain() {
        return getEnvironmentVariableOrDefaultValue("ENTITLEMENTS_DOMAIN", DEFAULT_ENTITLEMENTS_DOMAIN);
    }

    public static String getAWSCognitoClientId() {
        return getEnvironmentVariableOrDefaultValue("AWS_COGNITO_CLIENT_ID", "");
    }

    public static String getAWSCognitoAuthFlow() {
        return getEnvironmentVariableOrDefaultValue("AWS_COGNITO_AUTH_FLOW", "");
    }

    public static String getAWSCognitoUser() {
        return getEnvironmentVariableOrDefaultValue("AWS_COGNITO_AUTH_PARAMS_USER", "");
    }

    public static String getAWSCognitoPassword() {
        return getEnvironmentVariableOrDefaultValue("AWS_COGNITO_AUTH_PARAMS_PASSWORD", "");
    }

    private static String getEnvironmentVariableOrDefaultValue(String key, String defaultValue) {
        String environmentVariable = getEnvironmentVariable(key);
        if (environmentVariable == null) {
            environmentVariable = defaultValue;
        }
        return environmentVariable;
    }

    private static String getEnvironmentVariable(String propertyKey) {
        return System.getProperty(propertyKey, System.getenv(propertyKey));
    }
}
