package org.opengeo.analytics.web;

import org.geoserver.monitor.Query;
import org.opengeo.analytics.command.SlowestRequestCommand;

public class SlowestRequestProvider extends RequestDataProvider {

    public SlowestRequestProvider(Query query) {
        this(query, 5);
    }
    
    public SlowestRequestProvider(Query query, int limit) {
        super(new SlowestRequestCommand(query, monitor(), limit).query(), ID, PATH, START_TIME, TOTAL_TIME);
    }
}
