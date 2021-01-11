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

package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.provider.interfaces.IKindsCache;
import org.opengroup.osdu.indexer.config.IndexerConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Set;

@Component
public class KindsCache implements IKindsCache<String, Set>, AutoCloseable {
    private RedisCache<String, Set> cache;

    @Inject
    public KindsCache(final IndexerConfigurationProperties properties) {
        cache = new RedisCache<>(properties.getRedisSearchHost(), Integer.parseInt(properties.getRedisSearchPort()),
                properties.getKindsCacheExpiration() * 60,
                properties.getKindsRedisDatabase(), String.class, Set.class);
    }

    @Override
    public void close() throws Exception {
        this.cache.close();
    }

    @Override
    public void put(String s, Set o) {
        this.cache.put(s, o);
    }

    @Override
    public Set get(String s) {
        return this.cache.get(s);
    }

    @Override
    public void delete(String s) {
        this.cache.delete(s);
    }

    @Override
    public void clearAll() {
        this.cache.clearAll();
    }
}
