package org.geotools.data.mongodb.geojson;

import org.geotools.data.mongodb.MongoDataStoreTest;

public class GeoJSONMongoDataStoreTest extends MongoDataStoreTest {

    public GeoJSONMongoDataStoreTest() {
        super(new GeoJSONMongoTestSetup());
    }

}
