package org.opengroup.osdu.response;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.opengroup.osdu.core.common.model.info.FeatureFlagStateResolver.FeatureFlagState;

@Data
@EqualsAndHashCode(callSuper = true)
public class InfoResponseMock extends ResponseBase {
  private String groupId;
  private String artifactId;
  private String version;
  private String buildTime;
  private String branch;
  private String commitId;
  private String commitMessage;
  private List<FeatureFlagState> featureFlagStates;
}
