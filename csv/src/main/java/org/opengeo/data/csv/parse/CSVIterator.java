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

    private GeometryDescriptor geometryDescriptor;

    public CSVIterator(CSVFileState csvFileState, SimpleFeatureType featureType) throws IOException {
        this.featureType = featureType;
        this.csvFileState = csvFileState;
        csvReader = csvFileState.openCSVReader();
        this.headers = csvReader.getHeaders();
        this.geometryFactory = new GeometryFactory();
        this.geometryDescriptor = featureType.getGeometryDescriptor();
    }

    // TODO this logic is spread through the latlon strategy and this class now
    // could consider consolidating it all into the strategy
    // would need to deal with the idx state in that case,
    // maybe just a builder instead of a feature is returned
    private SimpleFeature buildFeature(String[] csvRecord) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        Double x = null, y = null;
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            if (i < csvRecord.length) {
                String value = csvRecord[i].trim();
                if (geometryDescriptor != null && "lat".equalsIgnoreCase(header)) {
                    y = Double.valueOf(value);
                } else if ((geometryDescriptor != null && "lon".equalsIgnoreCase(header)) ||
                           (geometryDescriptor != null && "lng".equalsIgnoreCase(header))) {
                    x = Double.valueOf(value);
                } else {
                    // geotools converters take care of converting for us
                    builder.set(header, value);
                }
            } else {
                builder.set(header, null);
            }
        }
        if (x != null && y != null && geometryDescriptor != null) {
            Coordinate coordinate = new Coordinate(x, y);
            Point point = geometryFactory.createPoint(coordinate);
            builder.set(geometryDescriptor.getLocalName(), point);
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
