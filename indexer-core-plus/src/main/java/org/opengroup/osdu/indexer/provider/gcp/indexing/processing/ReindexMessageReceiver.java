/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.indexer.api.RecordIndexerApi;
import org.opengroup.osdu.indexer.provider.gcp.indexing.scope.ThreadDpsHeaders;
import org.springframework.http.ResponseEntity;

@Slf4j
public class ReindexMessageReceiver extends IndexerOqmMessageReceiver {

  private final RecordIndexerApi recordIndexerApi;

  public ReindexMessageReceiver(ThreadDpsHeaders dpsHeaders, TokenProvider tokenProvider, RecordIndexerApi recordIndexerApi) {
    super(dpsHeaders, tokenProvider);
    this.recordIndexerApi = recordIndexerApi;
  }

  @Override
  protected void sendMessage(OqmMessage oqmMessage) throws Exception {
    RecordChangedMessages indexWorkerRequestBody = getIndexWorkerRequestBody(oqmMessage);
    log.debug("Reindex job message body: {}", indexWorkerRequestBody);
    ResponseEntity<JobStatus> jobStatusResponse = recordIndexerApi.indexWorker(indexWorkerRequestBody);
    log.debug("Job status: {}", jobStatusResponse);
  }

  private RecordChangedMessages getIndexWorkerRequestBody(OqmMessage request) {
    RecordChangedMessages recordChangedMessages = new RecordChangedMessages();
    recordChangedMessages.setMessageId(dpsHeaders.getCorrelationId());
    recordChangedMessages.setData(request.getData());
    recordChangedMessages.setAttributes(request.getAttributes());
    recordChangedMessages.setPublishTime(LocalDateTime.now().toString());
    return recordChangedMessages;
  }
}
