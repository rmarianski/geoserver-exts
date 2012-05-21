package org.opengeo.data.csv;

import java.net.URI;

import org.opengeo.data.csv.parse.CSVStrategy;
import org.opengeo.data.csv.parse.LatLonCSVStrategy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class LatLonStrategyFactory implements CSVStrategyFactory {

    private final String name;

    private final CoordinateReferenceSystem crs;

    private final URI namespace;

    public LatLonStrategyFactory(String name, CoordinateReferenceSystem crs, URI namespace) {
        this.name = name;
        this.crs = crs;
        this.namespace = namespace;
    }

    @Override
    public CSVStrategy createCSVStrategy(String[] headers) {
        return new LatLonCSVStrategy(name, crs, headers, namespace);
    }

}
