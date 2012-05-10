package org.opengeo.analytics.web;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.opengeo.analytics.command.RecentRequestCommand;

public class RecentRequestProvider extends RequestDataProvider {

    public RecentRequestProvider(Query query) {
        this(query, 5);
    }
    
    public RecentRequestProvider(Query query, int limit) {
        super(new RecentRequestCommand(query, monitor(), limit).query(), ID, STATUS, PATH, START_TIME);
    }
}
