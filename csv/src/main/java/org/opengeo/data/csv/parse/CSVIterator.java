package org.opengeo.data.csv.parse;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengeo.data.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.csvreader.CsvReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CSVIterator implements Iterator<SimpleFeature> {

    private int idx = 1;

    private SimpleFeature next = null;

    private final CsvReader csvReader;

    private final SimpleFeatureType featureType;

    private String[] headers;

    private GeometryFactory geometryFactory;

    private CSVFileState csvFileState;

    private String geometryName;

    public CSVIterator(CSVFileState csvFileState, SimpleFeatureType featureType) throws IOException {
        this.featureType = featureType;
        this.csvFileState = csvFileState;
        csvReader = csvFileState.openCSVReader();
        this.headers = csvReader.getHeaders();
        this.geometryFactory = new GeometryFactory();
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        this.geometryName = geometryDescriptor.getLocalName();
    }

    private SimpleFeature buildFeature(String[] csvRecord) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        Double x = null, y = null;
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            String value = csvRecord[i].trim();
            if ("lat".equalsIgnoreCase(header)) {
                y = Double.valueOf(value);
            } else if ("lon".equalsIgnoreCase(header)) {
                x = Double.valueOf(value);
            } else {
                builder.set(header, value);
            }
        }
        if (x != null && y != null) {
            Coordinate coordinate = new Coordinate(x, y);
            Point point = geometryFactory.createPoint(coordinate);
            builder.set(geometryName, point);
        }
        return builder.buildFeature(csvFileState.getTypeName() + "-" + idx++);

    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        try {
            next = readFeature();
        } catch (IOException e) {
            next = null;
        }
        return next != null;
    }

    private SimpleFeature readFeature() throws IOException {
        if (csvReader.readRecord()) {
            String[] csvRecord = csvReader.getValues();
            return buildFeature(csvRecord);
        }
        return null;
    }

    @Override
    public SimpleFeature next() {
        if (next != null) {
            SimpleFeature result = next;
            next = null;
            return result;
        }
        SimpleFeature feature;
        try {
            feature = readFeature();
        } catch (IOException e) {
            feature = null;
        }
        if (feature == null) {
            throw new NoSuchElementException();
        }
        return feature;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove features from csv iteratore");
    }

    public void close() {
        csvReader.close();
    }

}
