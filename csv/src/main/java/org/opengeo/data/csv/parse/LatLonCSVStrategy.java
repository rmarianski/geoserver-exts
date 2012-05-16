package org.opengeo.data.csv.parse;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class LatLonCSVStrategy implements CSVStrategy {

    private static final String GEOMETRY_COLUMN = "location";

    private String name;

    private CoordinateReferenceSystem crs;

    private int idx;

    private String[] headers;

    private SimpleFeatureType featureType;

    public LatLonCSVStrategy(String name, CoordinateReferenceSystem crs, String[] headers) {
        this.name = name;
        this.crs = crs;
        this.headers = headers;
        this.idx = 1;
        this.featureType = buildFeatureType();
    }

    private SimpleFeatureType buildFeatureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(this.name);
        builder.setCRS(this.crs);
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
        return featureType;
    }

    @Override
    public SimpleFeature buildFeature(String[] csvRecord) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        Coordinate coordinate = new Coordinate();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            String value = csvRecord[i].trim();
            if ("lat".equalsIgnoreCase(header)) {
                coordinate.y = Double.valueOf(value);
            } else if ("lon".equalsIgnoreCase(header)) {
                coordinate.x = Double.valueOf(value);
            } else {
                builder.set(header, value);
            }
        }
        builder.set(GEOMETRY_COLUMN, coordinate);
        return builder.buildFeature(name + this.idx++);
    }
}
