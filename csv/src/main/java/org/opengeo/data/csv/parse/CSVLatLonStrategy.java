package org.opengeo.data.csv.parse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengeo.data.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.csvreader.CsvReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CSVLatLonStrategy implements CSVStrategy {

    private static final String GEOMETRY_COLUMN = "location";

    private volatile SimpleFeatureType featureType;

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
            if (isLatitude(col)) {
                seenLat = true;
                latSpelling = col;
                if (type == Double.class || type == Integer.class) {
                    validLat = true;
                }
            } else if (isLongitude(col)) {
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
            String[] record = csvReader.getValues();
            List<String> values = Arrays.asList(record);
            if (record.length >= headers.length) {
                values = values.subList(0, headers.length);
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
        return new CSVIterator(csvFileState, this);
    }

    @Override
    public SimpleFeature createFeature(String recordId, String[] csvRecord) {
        SimpleFeatureType featureType = getFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        GeometryFactory geometryFactory = new GeometryFactory();
        Double x = null, y = null;
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            if (i < csvRecord.length) {
                String value = csvRecord[i].trim();
                if (geometryDescriptor != null && isLatitude(header)) {
                    y = Double.valueOf(value);
                } else if (geometryDescriptor != null && isLongitude(header)) {
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
        return builder.buildFeature(csvFileState.getTypeName() + "-" + recordId);
    }

    private boolean isLatitude(String s) {
        return "latitude".equalsIgnoreCase(s) || "lat".equalsIgnoreCase(s);
    }

    private boolean isLongitude(String s) {
        return "lon".equalsIgnoreCase(s) || "lng".equalsIgnoreCase(s) || "long".equalsIgnoreCase(s)
                || "longitude".equalsIgnoreCase(s);
    }
}
