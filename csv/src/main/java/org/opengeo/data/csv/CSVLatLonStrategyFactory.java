package org.opengeo.data.csv;

import org.opengeo.data.csv.parse.CSVLatLonStrategy;
import org.opengeo.data.csv.parse.CSVStrategy;

public class CSVLatLonStrategyFactory implements CSVStrategyFactory {

    private final CSVFileState csvFileState;

    public CSVLatLonStrategyFactory(CSVFileState csvFileState) {
        this.csvFileState = csvFileState;
    }

    @Override
    public CSVStrategy createCSVStrategy() {
        return new CSVLatLonStrategy(csvFileState);
    }

}
