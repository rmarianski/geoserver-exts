package org.opengeo.analytics.command;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.Query.SortOrder;
import org.opengeo.analytics.RequestOriginSummary;
import org.opengeo.analytics.ResourceSummary;

public class CommonOriginCommand extends AbstractCommand<List<RequestOriginSummary>> {
    
    int offset;
    int count;
    
    public CommonOriginCommand(Query query, Monitor monitor, int offset, int count) {
        super(query, monitor);
        this.offset = offset;
        this.count = count;
    }
    
    @Override
    public Query query() {
        Query q = query.clone();
        q.getProperties().clear();
        q.getAggregates().clear();
        q.getGroupBy().clear();
        
        q.properties("remoteAddr", "remoteHost", "remoteLat", "remoteLon", 
            "remoteCountry", "remoteCity").aggregate("count()");
        q.group("remoteAddr", "remoteHost", "remoteLat", "remoteLon", 
            "remoteCountry", "remoteCity");
        q.sort("count()", SortOrder.DESC);
        
        if (offset > -1) {
            q.setOffset((long) offset);
        }
        if (count > -1) {
            q.setCount((long)count);
        }
        //q.page(0l, (long)n);
        
        return q;
    }
    
    @Override
    public List<RequestOriginSummary> execute() {
        final long nrequests = new CountRequestCommand(query, monitor).execute();
        
        final List<RequestOriginSummary> list = new ArrayList();
        
        if (nrequests > 0) {
            monitor.query(query(), new RequestDataVisitor() {
                public void visit(RequestData data, Object... aggregates) {
                    RequestOriginSummary summary = new RequestOriginSummary();
                    summary.setIP(data.getRemoteAddr());
                    summary.setHost(data.getRemoteHost());
                    summary.setCountry(data.getRemoteCountry());
                    summary.setCity(data.getRemoteCity());
                    
                    summary.setCount(((Number)aggregates[0]).longValue());
                    summary.setPercent((summary.getCount() / ((double)nrequests))*100);
                    list.add(summary);
                }
            });
        }
        
        return list;
    }

}
