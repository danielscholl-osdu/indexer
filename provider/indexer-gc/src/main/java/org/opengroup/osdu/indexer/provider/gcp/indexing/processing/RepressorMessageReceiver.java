/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.indexer.provider.gcp.indexing.processing;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.model.indexer.RecordReindexRequest;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.indexer.api.ReindexApi;
import org.opengroup.osdu.indexer.provider.gcp.indexing.scope.ThreadDpsHeaders;
import org.springframework.http.ResponseEntity;

@Slf4j
public class RepressorMessageReceiver extends IndexerOqmMessageReceiver {

  private final Gson gson = new Gson();
  private final ReindexApi reindexApi;

  public RepressorMessageReceiver(ThreadDpsHeaders dpsHeaders, TokenProvider tokenProvider,
      ReindexApi reindexApi) {
    super(dpsHeaders, tokenProvider);
    this.reindexApi = reindexApi;
  }

  @Override
  protected void sendMessage(OqmMessage oqmMessage) throws Exception {
    RecordReindexRequest reindexBody = getReindexBody(oqmMessage);
    log.debug("Reindex job message body: {}", reindexBody);
    ResponseEntity<?> reindexResponse = reindexApi.reindex(reindexBody, false);
    log.debug("Reindex job status: {}", reindexResponse);
  }

  private RecordReindexRequest getReindexBody(OqmMessage request) {
    return this.gson.fromJson(request.getData(), RecordReindexRequest.class);
  }
}
