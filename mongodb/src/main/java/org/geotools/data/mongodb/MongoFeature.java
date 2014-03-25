package org.geotools.data.mongodb;

import java.util.List;

import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.simple.SimpleFeatureType;

public class MongoFeature extends SimpleFeatureImpl {

    public MongoFeature(Object[] values, SimpleFeatureType featureType, String id) {
        super(values, featureType, new FeatureIdImpl(id), false);
    }

}
