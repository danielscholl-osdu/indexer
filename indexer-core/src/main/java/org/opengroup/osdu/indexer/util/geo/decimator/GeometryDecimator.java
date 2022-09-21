package org.opengroup.osdu.indexer.util.geo.decimator;

import org.opengroup.osdu.indexer.model.geojson.*;

import java.util.ArrayList;
import java.util.List;

public class GeometryDecimator {
    private static final double NormalShapeDecimationEpsilon = 10; // meters
    private static final double ThumbnailShapeDecimationEpsilon = 1000; // meters
    private static final double DegreesToMeters = 100000; // approximate using 100km per degree
    private static final int MaxShapePointCountForLineDecimation = 300000;

    public boolean decimate(GeometryCollection geometryCollection) {
        return decimate(geometryCollection, NormalShapeDecimationEpsilon);
    }

    public boolean decimateAsThumbnail(GeometryCollection geometryCollection) {
        return decimate(geometryCollection, ThumbnailShapeDecimationEpsilon);
    }

    private boolean decimate(GeometryCollection geometryCollection, double epsilon) {
        if(geometryCollection == null || geometryCollection.getGeometries() == null)
            return false;

        boolean decimated = false;
        for(GeoJsonObject geoJsonObject: geometryCollection.getGeometries()) {
            decimated |= decimateBasicGeometry(geoJsonObject, epsilon);
        }
        return decimated;
    }

    private boolean decimate(MultiLineString geometry, double epsilon) {
        if(geometry == null || geometry.getCoordinates() == null)
            return false;

        boolean decimated = false;
        for(List<Position> coordinates : geometry.getCoordinates()) {
            decimated |= decimateLine(coordinates, epsilon);
        }
        return decimated;
    }

    private boolean decimate(LineString geometry, double epsilon) {
        if(geometry == null)
            return false;

        return decimateLine(geometry.getCoordinates(), epsilon);
    }

    private boolean decimate(MultiPolygon geometry, double epsilon) {
        if(geometry == null || geometry.getCoordinates() == null)
            return false;

        boolean decimated = false;
        for(List<List<Position>> polygon : geometry.getCoordinates()) {
            for(List<Position> coordinates : polygon) {
                decimated |= decimateLine(coordinates, epsilon);
            }
        }
        return decimated;
    }

    private boolean decimate(Polygon geometry, double epsilon) {
        if(geometry == null || geometry.getCoordinates() == null)
            return false;

        boolean decimated = false;
        for(List<Position> coordinates : geometry.getCoordinates()) {
            decimated |= decimateLine(coordinates, epsilon);
        }
        return decimated;
    }

    private boolean decimate(Point geometry, double epsilon) {
        return false;
    }

    private boolean decimate(MultiPoint geometry, double epsilon) {
        return false;
    }

    private boolean decimateBasicGeometry(GeoJsonObject geometry, double epsilon) {
        if(geometry instanceof MultiLineString) {
            return decimate((MultiLineString) geometry, epsilon);
        }
        else if(geometry instanceof LineString) {
            return decimate((LineString)geometry, epsilon);
        }
        else if(geometry instanceof MultiPolygon) {
            return decimate((MultiPolygon)geometry, epsilon);
        }
        else if(geometry instanceof Polygon) {
            return decimate((Polygon)geometry, epsilon);
        }
        else if(geometry instanceof Point) {
            return decimate((Point)geometry, epsilon);
        }
        else if(geometry instanceof MultiPoint) {
            return decimate((MultiPoint)geometry, epsilon);
        }
        else
            return false;
    }

    private boolean decimateLine(List<Position> coordinates, double epsilon) {
        if(coordinates == null || coordinates.size() < 3)
            return false;

        // Douglas/Peucker algorithm is expensive, apply simple sampling if the line has too many points
        coordinates = downSamplePoints(coordinates);

        List<Integer> pointIndexes = DouglasPeuckerReducer.getPointIndexesToKeep(coordinates, DegreesToMeters, epsilon);
        List<Position> sampledCoordinates = new ArrayList<>();
        for(int i : pointIndexes) {
            sampledCoordinates.add(coordinates.get(i));
        }

        boolean decimated = (coordinates.size() != sampledCoordinates.size());
        if(decimated) {
            coordinates.clear();
            coordinates.addAll(sampledCoordinates);
        }
        return decimated;
    }

    private List<Position> downSamplePoints(List<Position> coordinates) {
        //Don't sample it if the number of point is not much larger than MaxShapePointCountForLineDecimation
        if (coordinates.size() <= MaxShapePointCountForLineDecimation * 1.2) {
            return coordinates;
        }

        List<Position> sampledPoints = new ArrayList<>();
        int interval = (int)Math.ceil(coordinates.size() / (double)MaxShapePointCountForLineDecimation);
        int i = 0;
        for(; i < coordinates.size(); i += interval) {
            sampledPoints.add(coordinates.get(i));
        }
        // Add the last point
        sampledPoints.add(coordinates.get(coordinates.size() -1));
        return sampledPoints;
    }


}
