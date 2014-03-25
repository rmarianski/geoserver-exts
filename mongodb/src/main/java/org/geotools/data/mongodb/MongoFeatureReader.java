package org.geotools.data.mongodb;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.geotools.data.simple.SimpleFeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MongoFeatureReader implements SimpleFeatureReader {

    DBCursor cursor;
    MongoFeatureSource featureSource;
    CollectionMapper mapper;

    public MongoFeatureReader(DBCursor cursor, MongoFeatureSource featureSource) {
        this.cursor = cursor;
        this.featureSource = featureSource;
        mapper = featureSource.getMapper();
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return featureSource.getSchema();
    }

    @Override
    public boolean hasNext() throws IOException {
        return cursor.hasNext();
    }

    @Override
    public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
        DBObject obj = cursor.next();

        return mapper.buildFeature(obj, featureSource.getSchema());
    }

    @Override
    public void close() throws IOException {
        cursor.close();
    }

}
