package org.opengroup.osdu.indexer.util.geo.decimator;

import lombok.Data;

import java.util.Map;

@Data
public class DecimatedResult {
    Map<String, Object> decimatedShapeObj;
    Map<String, Object> thumbnailShapeObj;
    boolean isDecimated = false;
}
