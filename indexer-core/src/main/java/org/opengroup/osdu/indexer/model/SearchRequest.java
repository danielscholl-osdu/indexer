/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.model;

import lombok.Data;
import lombok.ToString;
import org.opengroup.osdu.core.common.model.search.SortQuery;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ToString
public class SearchRequest {
    @NotNull(message = "Kind is missing")
    private Object kind;
    private String query;
    private int limit;
    private int offset;
    private String cursor;
    private List<String> returnedFields;
    private SortQuery sort;
    private boolean trackTotalCount;
}
