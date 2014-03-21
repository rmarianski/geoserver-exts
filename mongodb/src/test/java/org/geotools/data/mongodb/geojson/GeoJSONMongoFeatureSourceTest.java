package org.geotools.data.mongodb.geojson;

import org.geotools.data.mongodb.MongoFeatureSourceTest;

public class GeoJSONMongoFeatureSourceTest extends MongoFeatureSourceTest {

    public GeoJSONMongoFeatureSourceTest() {
        super(new GeoJSONMongoTestSetup());
    }

}
