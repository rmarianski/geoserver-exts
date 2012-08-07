package org.opengeo.data.csv.parse;

public class CSVTestStrategySupport {

    public static String buildInputString(String... rows) {
        StringBuilder builder = new StringBuilder();
        for (String row : rows) {
            builder.append(row);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

}
