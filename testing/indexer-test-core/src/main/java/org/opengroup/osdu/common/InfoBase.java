package org.opengroup.osdu.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.opengroup.osdu.response.FeatureFlagStateMock;
import org.opengroup.osdu.response.InfoResponseMock;
import org.opengroup.osdu.util.Config;
import org.opengroup.osdu.util.ElasticUtils;
import org.opengroup.osdu.util.HTTPClient;

@Slf4j
public class InfoBase extends TestsBase {

  // Feature flag property constant - matches the value used in FeatureConstants
  private static final String EXPOSE_FEATUREFLAG_ENABLED_PROPERTY = "expose_featureflag.enabled";

  private static final List<String> expectedFeatureFlags = List.of(
      "featureFlag.mapBooleanToString.enabled",
      "featureFlag.asIngestedCoordinates.enabled",
      "featureFlag.keywordLower.enabled",
      "featureFlag.bagOfWords.enabled",
      "collaborations-enabled",
      "custom-index-analyzer-enabled",
      "index-augmenter-enabled"
  );

  protected Map<String, String> headers = new HashMap<>();
  private InfoResponseMock response;

  public InfoBase(HTTPClient httpClient) {
    super(httpClient);
  }

  public InfoBase(HTTPClient httpClient, ElasticUtils elasticUtils) {
    super(httpClient, elasticUtils);
  }

  @Override
  protected String getApi() {
    return Config.getIndexerBaseURL() + "info";
  }

  @Override
  protected String getHttpMethod() {
    return "GET";
  }

  public void i_send_get_request_to_version_info_endpoint() {
    if (Config.getIndexerBaseURL().isEmpty()) {
      log.warn("Env variable INDEXER_HOST is empty. Version info endpoint test is skipped");
      return;
    }

    response =
            executeQuery(
                this.getApi(),
                Strings.EMPTY,
                headers,
                httpClient.getAccessToken(),
                InfoResponseMock.class);
  }

  public void i_send_get_request_to_version_info_endpoint_with_trailing_slash() {
    response =
            executeQuery(getApi()+"/", Strings.EMPTY, headers, httpClient.getAccessToken(), InfoResponseMock.class);
  }

  public void i_should_get_version_info_in_response() {
    if (response != null) {
      assertEquals(200, response.getResponseCode());
      assertNotNull(response.getGroupId());
      assertNotNull(response.getArtifactId());
      assertNotNull(response.getVersion());
      assertNotNull(response.getBuildTime());
      assertNotNull(response.getBranch());
      assertNotNull(response.getCommitId());
      assertNotNull(response.getCommitMessage());
      List<FeatureFlagStateMock> featureFlagStates = response.getFeatureFlagStates();

      // Read the actual configuration property value to validate behavior alignment
      // Check system property first, then fall back to environment variable
      String featureFlagExposeEnabledProperty = System.getProperty(EXPOSE_FEATUREFLAG_ENABLED_PROPERTY);
      if (featureFlagExposeEnabledProperty == null) {
          featureFlagExposeEnabledProperty = System.getenv("EXPOSE_FEATUREFLAG_ENABLED");
      }
      boolean isFeatureFlagExposureEnabled = featureFlagExposeEnabledProperty == null || !"false".equalsIgnoreCase(featureFlagExposeEnabledProperty);
      
      if (!isFeatureFlagExposureEnabled)
      {
        assertNull(featureFlagStates);
      }
      else
      {
        assertNotNull(featureFlagStates);
        assertFalse(featureFlagStates.isEmpty());
        for (String ffName : expectedFeatureFlags){
          assertTrue(featureFlagStates.stream().anyMatch(ffState -> ffState.getName().equals(ffName)));    
        }    
      }
    } else {
      log.warn("Version info endpoint provided null response");
    }
  }
}
