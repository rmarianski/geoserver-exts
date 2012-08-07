package org.opengeo.data.csv;

import org.opengeo.data.csv.parse.CSVAttributesOnlyStrategy;
import org.opengeo.data.csv.parse.CSVStrategy;

public class CSVAttributesOnlyStrategyFactory implements CSVStrategyFactory {

    private final CSVFileState csvFileState;

    public CSVAttributesOnlyStrategyFactory(CSVFileState csvFileState) {
        this.csvFileState = csvFileState;
    }

    @Override
    public CSVStrategy createCSVStrategy() {
        return new CSVAttributesOnlyStrategy(csvFileState);
    }

}
