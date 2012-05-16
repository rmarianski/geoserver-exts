package org.opengeo.data.csv;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.opengeo.data.csv.parse.CSVStrategy;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.csvreader.CsvReader;

public class CSVFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

    private CsvReader csvReader;

    private CSVStrategy csvStrategy;

    private SimpleFeature next;

    private SimpleFeatureType featureType;

    private Filter filter;

    public CSVFeatureReader(CsvReader csvReader, CSVStrategy csvStrategy) throws IOException {
        this(csvReader, csvStrategy, Query.ALL);
    }

    public CSVFeatureReader(CsvReader csvReader, CSVStrategy csvStrategy, Query query)
            throws IOException {
        this.csvReader = csvReader;
        this.csvStrategy = csvStrategy;
        this.featureType = csvStrategy.getFeatureType();
        this.filter = query.getFilter();
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return featureType;
    }

    private SimpleFeature readFeature() throws IOException {
        if (csvReader.readRecord()) {
            String[] csvRecord = csvReader.getValues();
            return csvStrategy.buildFeature(csvRecord);
        }
        return null;
    }

    @Override
    public SimpleFeature next() throws IOException {
        if (next != null) {
            SimpleFeature result = next;
            next = null;
            return result;
        }
        SimpleFeature feature = readFeature();
        if (feature == null) {
            throw new NoSuchElementException();
        }
        return feature;
    }

    @Override
    public boolean hasNext() throws IOException {
        if (next != null) {
            return true;
        }
        next = readFeature();
        while (next != null && !filter.evaluate(next)) {
            next = readFeature();
        }
        return next != null;
    }

    @Override
    public void close() throws IOException {
        csvReader.close();
        next = null;
    }

}
