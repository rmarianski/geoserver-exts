package org.opengeo.data.csv;

import org.opengeo.data.csv.parse.CSVStrategy;

public interface CSVStrategyFactory {

    public CSVStrategy createCSVStrategy(String[] headers);

}
