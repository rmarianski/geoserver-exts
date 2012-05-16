package org.opengeo.data.csv;

import org.opengeo.data.csv.parse.CSVStrategy;
import org.opengeo.data.csv.parse.LatLonCSVStrategy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class LatLonStrategyFactory implements CSVStrategyFactory {

    private final String name;

    private final CoordinateReferenceSystem crs;

    public LatLonStrategyFactory(String name, CoordinateReferenceSystem crs) {
        this.name = name;
        this.crs = crs;
    }

    @Override
    public CSVStrategy createCSVStrategy(String[] headers) {
        return new LatLonCSVStrategy(name, crs, headers);
    }

}
