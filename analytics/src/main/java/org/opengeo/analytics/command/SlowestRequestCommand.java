package org.opengeo.analytics.command;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.SortOrder;

public class SlowestRequestCommand extends AbstractCommand {

    int n;
    
    public SlowestRequestCommand(Query query, Monitor monitor, int n) {
        super(query, monitor);
        this.n = n;
    }

    @Override
    public Query query() {
        Query q = query.clone();
        q.getProperties().clear();
        
        //TODO: specify properties elsewhere
        q.properties("id", "path", "startTime", "totalTime");
        if (n > 0) {
            q.setCount(q.getCount() != null ? Math.min(q.getCount(), n) : n);
        }
        q.sort("totalTime", SortOrder.DESC);
        return q;
    }

}
