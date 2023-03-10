package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.indexer.provider.interfaces.ISchemaCache;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.inject.Inject;

@Component
@RequestScope
public class PartitionSafeSchemaCache extends AbstractPartitionSafeCache<String, String> {
    @Inject
    private ISchemaCache schemaCache;

    @Override
    public void put(String s, String o) {
        this.schemaCache.put(cacheKey(s), o);
    }

    @Override
    public String get(String s) {
        return (String)this.schemaCache.get(cacheKey(s));
    }

    @Override
    public void delete(String s) {
        this.schemaCache.delete(cacheKey(s));
    }

    @Override
    public void clearAll() {
        this.schemaCache.clearAll();
    }
}
