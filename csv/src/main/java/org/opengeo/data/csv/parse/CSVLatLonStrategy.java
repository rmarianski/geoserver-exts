package org.opengeo.data.csv.parse;

import java.io.IOException;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengeo.data.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeatureType;

import com.csvreader.CsvReader;
import com.vividsolutions.jts.geom.Point;

public class CSVLatLonStrategy implements CSVStrategy {

    private static final String GEOMETRY_COLUMN = "location";

    private SimpleFeatureType featureType;

    private String[] headers;

    private final CSVFileState csvFileState;

    public CSVLatLonStrategy(CSVFileState csvFileState) {
        this.csvFileState = csvFileState;
        featureType = null;
    }

    private SimpleFeatureType buildFeatureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(csvFileState.getTypeName());
        builder.setCRS(csvFileState.getCrs());

        try {
            CsvReader csvReader = csvFileState.openCSVReader();
            headers = csvReader.getHeaders();
            csvReader.close();
        } catch (IOException e) {
            throw new RuntimeException("Failure reading csv file", e);
        }
        if (csvFileState.getNamespace() != null) {
            builder.setNamespaceURI(csvFileState.getNamespace());
        }
        builder.add(GEOMETRY_COLUMN, Point.class);

        for (String col : headers) {
            if ("lat".equalsIgnoreCase(col) || "lon".equalsIgnoreCase(col)) {
                continue;
            }
            builder.add(col, String.class);
        }
        return builder.buildFeatureType();
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        if (featureType == null) {
            synchronized (this) {
                if (featureType == null) {
                    featureType = buildFeatureType();
                }
            }
        }
        return featureType;
    }

    @Override
    public CSVIterator iterator() throws IOException {
        return new CSVIterator(csvFileState, getFeatureType());
    }

}
