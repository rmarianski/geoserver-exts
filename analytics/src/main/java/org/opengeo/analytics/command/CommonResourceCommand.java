package org.opengeo.analytics.command;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.opengeo.analytics.ResourceSummary;

public class CommonResourceCommand extends AbstractCommand<List<ResourceSummary>> {

    int offset;
    int count;
    public CommonResourceCommand(Query query, Monitor monitor, int offset, int count) {
        super(query, monitor);
        this.offset = offset;
        this.count = count;
    }

    @Override
    public Query query() {
        Query q = query.clone();
        
        q.getProperties().clear();
        q.getAggregates().clear();
        
        q.properties("resource").aggregate("count()")
            .filter("resource", null, Comparison.NEQ).group("resource")
            .sort("count()", SortOrder.DESC);
        
        if (offset > -1) {
            q.setOffset((long) offset);
        }
        if (count > -1) {
            q.setCount((long)count);
        }
        
        return q;
    }
    
    @Override
    public List<ResourceSummary> execute() {
        final long nrequests = new CountRequestCommand(query, monitor).execute();
        
        final List<ResourceSummary> list = new ArrayList();
        
        if (nrequests > 0) {
            monitor.query(query(), new RequestDataVisitor() {
                public void visit(RequestData data, Object... aggregates) {
                    ResourceSummary summary = new ResourceSummary();
                    summary.setResource(data.getResources().get(0));
                    summary.setCount(((Number)aggregates[0]).longValue());
                    summary.setPercent((summary.getCount() / ((double)nrequests)) * 100);
                    list.add(summary);
                }
            });
            new RequestSummaryCommand(query, monitor, list).execute();
        }
        
        return list;
    }

}
