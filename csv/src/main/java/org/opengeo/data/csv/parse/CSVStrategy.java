package org.opengeo.data.csv.parse;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public interface CSVStrategy {
    public SimpleFeatureType getFeatureType();

    public SimpleFeature buildFeature(String[] csvRecord);
}
