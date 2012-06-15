package org.opengeo.data.csv.parse;

import java.io.IOException;

import org.opengis.feature.simple.SimpleFeatureType;

public interface CSVStrategy {
    public SimpleFeatureType getFeatureType();

    public CSVIterator iterator() throws IOException;
}
