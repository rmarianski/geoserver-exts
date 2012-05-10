package org.opengeo.analytics.web;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.opengeo.analytics.command.RecentFailedRequestCommand;

public class RecentFailedRequestProvider extends RequestDataProvider {

    public RecentFailedRequestProvider(Query query) {
        this(query, 5);
    }
    
    public RecentFailedRequestProvider(Query query, int limit) {
        super(new RecentFailedRequestCommand(query, monitor(), limit).query(), ID, STATUS, PATH, START_TIME);
    }

}
