package org.opengroup.osdu.indexer.util.function;

import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.indexer.model.indexproperty.ValueExtraction;

import java.util.List;
import java.util.Map;

public interface IAugmenterFunction {
    boolean isMatched(ValueExtraction valueExtraction);
    List<String> getValuePaths(ValueExtraction valueExtraction);
    List<SchemaItem> getExtendedSchemaItems(String extendedPropertyName);
    Map<String, Object> getPropertyValues(String extendedPropertyName, ValueExtraction valueExtraction, Map<String, Object> originalPropertyValues);
}
