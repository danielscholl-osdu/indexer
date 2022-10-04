// Copyright © Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.model.geojson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.indexer.model.geojson.jackson.GeoJsonConstants;
import org.opengroup.osdu.indexer.model.geojson.jackson.PointDeserializer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = PointDeserializer.class)
public class Point extends GeoJsonObject implements Positioned {

    private Position coordinates;

    public Point(double longitude, double latitude) {
        coordinates = new Position(longitude, latitude);
    }

    public Point(double longitude, double latitude, double altitude) {
        coordinates = new Position(longitude, latitude, altitude);
    }

    @Override
    public String getType() {
        return GeoJsonConstants.POINT;
    }
}
