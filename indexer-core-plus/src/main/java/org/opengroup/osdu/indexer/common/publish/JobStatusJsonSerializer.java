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

package org.opengroup.osdu.indexer.common.publish;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.springframework.stereotype.Component;

@Component
public class JobStatusJsonSerializer implements JsonSerializer<JobStatus> {

  private final Gson gson = new Gson();

  @Override
  public JsonElement serialize(JobStatus src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.add("recordsStatus", this.gson.toJsonTree(src.getStatusesList()));
    jsonObject.add("debugInfo", this.gson.toJsonTree(src.getDebugInfos()));
    return jsonObject;
  }
}
