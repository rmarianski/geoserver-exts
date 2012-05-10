package org.opengeo.analytics.command;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.RequestData.Status;

public class RecentFailedRequestCommand extends RecentRequestCommand {

    public RecentFailedRequestCommand(Query query, Monitor monitor, int n) {
        super(query, monitor, n);
    }

    @Override
    public Query query() {
        Query q = super.query();
        q.getProperties().clear();
        
        //TODO: specify properties elsewhere
        q.properties("id", "status", "path", "startTime");
        q.filter("status", Status.FAILED, Comparison.EQ);
        return q;
    }

}
