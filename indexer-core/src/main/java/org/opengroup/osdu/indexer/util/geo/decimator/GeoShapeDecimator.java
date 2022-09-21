package org.opengroup.osdu.indexer.util.geo.decimator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.opengroup.osdu.indexer.model.geojson.*;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class GeoShapeDecimator {
    private ObjectMapper deserializerMapper;
    private ObjectMapper serializerMapper;
    private GeometryDecimator decimator;


    public GeoShapeDecimator() {
        decimator = new GeometryDecimator();
        serializerMapper = new ObjectMapper();
        deserializerMapper = createDeserializerMapper();
    }

    public DecimatedResult decimateShapeObj(Map<String, Object> shapeObj) throws JsonProcessingException {
        DecimatedResult result = new DecimatedResult();
        String type = (String)shapeObj.getOrDefault("type", null);
        if(type != null && type.equals("geometrycollection")) {
            GeometryCollection geometryCollection = deserializerMapper.readValue(deserializerMapper.writeValueAsString(shapeObj), GeometryCollection.class);
            boolean decimated = decimator.decimate(geometryCollection);
            if (decimated) {
                result.setDecimatedShapeObj(
                        serializerMapper.readValue(serializerMapper.writeValueAsString(geometryCollection), new TypeReference<Map<String, Object>>() {}));
                result.setDecimated(true);
            }
            // Must serialize the normal decimated shape before decimating it as thumbnail
            decimator.decimateAsThumbnail(geometryCollection);
            result.setThumbnailShapeObj(
                    serializerMapper.readValue(serializerMapper.writeValueAsString(geometryCollection), new TypeReference<Map<String, Object>>() {}));
        }

        return result;
    }

    @NotNull
    private static ObjectMapper createDeserializerMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(new NamedType(GeometryCollection.class, "geometrycollection"));
        mapper.registerSubtypes(new NamedType(Polygon.class, "polygon"));
        mapper.registerSubtypes(new NamedType(MultiPolygon.class, "multipolygon"));
        mapper.registerSubtypes(new NamedType(LineString.class, "linestring"));
        mapper.registerSubtypes(new NamedType(MultiLineString.class, "multilinestring"));
        mapper.registerSubtypes(new NamedType(Point.class, "point"));
        mapper.registerSubtypes(new NamedType(MultiPoint.class, "multipoint"));
        return mapper;
    }
}
