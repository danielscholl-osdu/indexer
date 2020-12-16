/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.opengroup.osdu.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opengroup.osdu.util.Config.getEntitlementsDomain;
import static org.opengroup.osdu.util.Config.getIndexerBaseURL;
import static org.opengroup.osdu.util.Config.getStorageBaseURL;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.ClientResponse;
import cucumber.api.DataTable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.HttpMethod;
import lombok.extern.java.Log;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.models.Setup;
import org.opengroup.osdu.models.TestIndex;
import org.opengroup.osdu.util.FileHandler;
import org.opengroup.osdu.util.HTTPClient;
import org.opengroup.osdu.util.ElasticUtils;

@Log
public class CleanupIndiciesSteps extends TestsBase {
  private final String timeStamp = String.valueOf(System.currentTimeMillis());
  private Map<String, TestIndex> inputIndexMap = new HashMap<>();
  private List<Map<String, Object>> records;
  private boolean shutDownHookAdded = false;
  private final Map<String, String> headers = httpClient.getCommonHeader();

  public CleanupIndiciesSteps(HTTPClient httpClient) {
    super(httpClient);
  }

  public CleanupIndiciesSteps(HTTPClient httpClient, ElasticUtils elasticUtils) {
    super(httpClient, elasticUtils);
  }


  public void theSchemaIsCreatedWithTheFollowingKind(DataTable dataTable) {
    List<Setup> inputList = dataTable.asList(Setup.class);
    for (Setup input : inputList) {
      TestIndex testIndex = getTextIndex();
      testIndex.setHttpClient(httpClient);
      testIndex.setIndex(generateActualNameWithTS(input.getIndex(), timeStamp));
      testIndex.setKind(generateActualNameWithTS(input.getKind(), timeStamp));
      testIndex.setSchemaFile(input.getSchemaFile());
      inputIndexMap.put(testIndex.getKind(), testIndex);
    }

    if (!shutDownHookAdded) {
      shutDownHookAdded = true;
      for (Map.Entry<String, TestIndex> kind : inputIndexMap.entrySet()) {
        kind.getValue().setupSchema();
      }
    }
  }

  public void iIngestRecordsWithTheforAGiven(String record, String dataGroup, String kind) {
    String actualKind = generateActualNameWithTS(kind, timeStamp);
    try {
      String fileContent = FileHandler.readFile(String.format("%s.%s", record, "json"));
      records = new Gson().fromJson(fileContent, new TypeToken<List<Map<String, Object>>>() {}.getType());

      for (Map<String, Object> testRecord : records) {
        testRecord.put("id", generateActualNameWithTS(testRecord.get("id").toString(), timeStamp));
        testRecord.put("kind", actualKind);
        testRecord.put("legal", generateLegalTag());
        String[] x_acl = {generateActualNameWithTS(dataGroup,timeStamp)+"."+getEntitlementsDomain()};
        Acl acl = Acl.builder().viewers(x_acl).owners(x_acl).build();
        testRecord.put("acl", acl);
      }
      String payLoad = new Gson().toJson(records);
      ClientResponse clientResponse = httpClient.send(HttpMethod.PUT, getStorageBaseURL() + "records", payLoad, headers, httpClient.getAccessToken());
      assertEquals(201, clientResponse.getStatus());
    } catch (Exception ex) {
      throw new AssertionError(ex.getMessage());
    }
  }

  public void iCheckThatTheIndexForHasBeenCreated(String kind) throws IOException, InterruptedException {
    assertTrue(isNewIndexCreated(generateActualNameWithTS(kind, timeStamp)));
  }

  public void iShouldDeleteTheRecordsForICreatedEarlier() {
    List<Map<String, Object>> deletedRecords = new ArrayList<>();
    if (records != null && !records.isEmpty()) {
      for (Map<String, Object> testRecord : records) {
        String id = testRecord.get("id").toString();
        ClientResponse clientResponse = httpClient.send(HttpMethod.DELETE, getStorageBaseURL()
            + "records/" + id, null, headers, httpClient.getAccessToken());
        if (clientResponse.getStatus() == 204) {
          deletedRecords.add(testRecord);
          log.info("Deleted the records with id " + id);
        }
      }
      assertEquals(records.size(), deletedRecords.size());
    }
  }

  public void iShouldDeleteTheSchemaForICreatedEarlier(String kind) {
    ClientResponse response = httpClient.send(HttpMethod.DELETE,
        String.format("%sschemas%s", getStorageBaseURL(), "/" + generateActualNameWithTS(kind, timeStamp)),null,
        headers, httpClient.getAccessToken());
    assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
  }

  public void iShouldCheckThetTheIndexforHasNotBeenDeleted(String kind) throws IOException, InterruptedException {
    assertTrue(isNewIndexExist(generateActualNameWithTS(kind, timeStamp)));
  }

  public void iShouldToRunCleanupOfIndexesForAnd(String kind, String message) {

    String url = getIndexerBaseURL() + "index-cleanup";
    log.info("URL: " + url);
    ClientResponse response = httpClient.send(HttpMethod.POST, url,
        convertMessageIntoJson(kind, message), headers, httpClient.getAccessToken());
    assertEquals(HttpStatus.SC_OK, response.getStatus());
  }

  public void iShouldCheckThatTheIndexForHasBeenDeleted(String kind) throws IOException, InterruptedException {
    assertFalse(isNewIndexExist(generateActualNameWithTS(kind, timeStamp)));
  }

  private String convertMessageIntoJson(String kind, String message) {
    String actualKind = generateActualNameWithTS(kind, timeStamp);
    RecordChangedMessages recordChangedMessages = (new Gson()).fromJson(String.format(message,
        actualKind, actualKind, timeStamp), RecordChangedMessages.class);
    return new Gson().toJson(recordChangedMessages);
  }

  private boolean isNewIndexExist(String index) throws IOException {
    return elasticUtils.isIndexExist(index.replace(":", "-"));
  }

  private boolean isNewIndexCreated(String index) throws IOException, InterruptedException {
    int iterator;
    boolean indexExist = false;

    // index.refresh_interval is set to default 30s, wait for 40s initially
    Thread.sleep(40000);

    for (iterator = 0; iterator < 20; iterator++) {
      indexExist = elasticUtils.isIndexExist(index.replace(":", "-"));
      if (indexExist) {
        break;
      } else {
        Thread.sleep(5000);
      }
      if ((iterator + 1) % 5 == 0) {
        elasticUtils.refreshIndex(index.replace(":", "-"));
      }
    }
    if (iterator >= 20) {
      fail(String.format("index not created after waiting for %s seconds", ((40000 + iterator * 5000) / 1000)));
    }
    return indexExist;
  }

  @Override
  protected String getApi() {
    return null;
  }

  @Override
  protected String getHttpMethod() {
    return null;
  }
}
