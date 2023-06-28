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

package org.opengroup.osdu.indexer.azure.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.model.storage.RecordData;
import org.opengroup.osdu.indexer.cache.IRelatedObjectCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Named;

@Component
@ConditionalOnProperty(value = "runtime.env.local", havingValue = "false", matchIfMissing = true)
public class RelatedObjectRedisCache  extends RedisCache<String, RecordData> implements IRelatedObjectCache {
    public RelatedObjectRedisCache(final @Named("REDIS_HOST") String host,
                                   final @Named("REDIS_PORT") int port,
                                   final @Named("REDIS_PASSWORD") String password,
                                   final @Named("RECORDS_REDIS_TTL") int timeout,
                                   @Qualifier("") @Value("${redis.database}") final int database) {
        super(host, port, password, timeout, database, String.class, RecordData.class);
    }
}
