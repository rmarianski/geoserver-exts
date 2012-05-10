package org.opengeo.analytics.command;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.opengeo.analytics.TimeAggregator;
import org.opengeo.analytics.View;

public class AggregateRequestByTime extends AbstractCommand<long[]> {
    
    View zoom;
    
    public AggregateRequestByTime(Query query, Monitor monitor, View zoom) {
        super(query, monitor);
        this.zoom = zoom;
    }

    @Override
    public Query query() {
        Query q = query.clone();
        q.getProperties().clear();
        q.getAggregates().clear();
        q.getGroupBy().clear();
        q.properties("startTime");
        return q;
    }

    public long[] execute() {
        Query q = query();
        
        TimeAggregator agg = new TimeAggregator(q.getFromDate(), q.getToDate(), zoom);
        monitor.query(query(), agg);
        
        return agg.getData();
    }
}
