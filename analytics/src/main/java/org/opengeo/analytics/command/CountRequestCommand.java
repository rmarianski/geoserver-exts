package org.opengeo.analytics.command;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;

public class CountRequestCommand extends AbstractCommand<Long> {

    public CountRequestCommand(Query query, Monitor monitor) {
        super(query, monitor);
    }

    @Override
    public Query query() {
        return query;
    }
    
    @Override
    public Long execute() {
        return monitor.getDAO().getCount(query());
    }
}
