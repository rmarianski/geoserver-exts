package org.opengeo.data.csv.parse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

        CsvReader csvReader = null;
        Map<String, Class<?>> typesFromData = null;

        try {
            csvReader = csvFileState.openCSVReader();
            headers = csvReader.getHeaders();
            // iterate through file to figure out the strictest types that can be used
            // what to do with rows that don't have same number of columns as header? skip? relax column types to allow nulls?
            // can you have a double type with nulls? returns back 0?
            typesFromData = findMostSpecificTypesFromData(csvReader, headers);
        } catch (IOException e) {
            throw new RuntimeException("Failure reading csv file", e);
        } finally {
            if (csvReader != null) {
                csvReader.close();
            }
        }
        if (csvFileState.getNamespace() != null) {
            builder.setNamespaceURI(csvFileState.getNamespace());
        }

        boolean validLat = false;
        boolean validLon = false;
        boolean seenLat = false;
        boolean seenLon = false;
        String latSpelling = null;
        String lonSpelling = null;
        for (String col : headers) {
            Class<?> type = typesFromData.get(col);
            if ("lat".equalsIgnoreCase(col)) {
                seenLat = true;
                latSpelling = col;
                if (type == Double.class || type == Integer.class) {
                    validLat = true;
                }
            } else if ("lon".equalsIgnoreCase(col) || "lng".equalsIgnoreCase(col)) {
                seenLon = true;
                lonSpelling = col;
                if (type == Double.class || type == Integer.class) {
                    validLon = true;
                }
            } else {
                builder.add(col, type);
            }
        }
        if (validLat && validLon) {
            builder.add(GEOMETRY_COLUMN, Point.class);
        } else {
            if (seenLat) {
                builder.add(latSpelling, typesFromData.get("lat"));
            }
            if (seenLon) {
                builder.add(lonSpelling, typesFromData.get("lon"));
            }
        }
        return builder.buildFeatureType();
    }

    private Map<String, Class<?>> findMostSpecificTypesFromData(CsvReader csvReader,
            String[] headers) throws IOException {
        Map<String, Class<?>> result = new HashMap<String, Class<?>>();
        // start off assuming Integers for everything
        for (String header : headers) {
            result.put(header, Integer.class);
        }
        while (csvReader.readRecord()) {
            String[] values = csvReader.getValues();
            // TODO should we skip these rows? or just pad with nulls/empty strings and make those strings?
            // can make nillable as appropriate
            if (values.length != headers.length) {
                continue;
            }
            int i = 0;
            for (String value : values) {
                String header = headers[i];
                Class<?> type = result.get(header);
                if (type == Integer.class) {
                    try {
                        Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        try {
                            Double.parseDouble(value);
                            type = Double.class;
                        } catch (NumberFormatException ex) {
                            type = String.class;
                        }
                    }
                } else if (type == Double.class) {
                    try {
                        Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        type = String.class;
                    }
                } else {
                    type = String.class;
                }
                result.put(header, type);
                i++;
            }
        }
        return result;
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
