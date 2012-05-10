package org.opengeo.analytics.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.Query.SortOrder;
import org.geoserver.monitor.RequestData;
import org.opengeo.analytics.RequestSummary;
import org.opengeo.analytics.ResourceSummary;

public class RequestSummaryCommand extends AbstractCommand<List<ResourceSummary>> {

    List<ResourceSummary> resources;
    
    public RequestSummaryCommand(Query query, Monitor monitor, List<ResourceSummary> resources) {
        super(query, monitor);
        this.resources = resources;
    }

    @Override
    public Query query() {
        Query q = query.clone();

        q.getProperties().clear();
        q.getAggregates().clear();

        List<String> list = new ArrayList();
        for (ResourceSummary s : resources) { list.add(s.getResource()); }
        
        q.properties("resource", "operation").aggregate("count()")
            .filter("resource", list, Comparison.IN).group("resource", "operation")
            .sort("count()", SortOrder.DESC);

        return q;
    }
    
    
    @Override
    public List<ResourceSummary> execute() {
        final Map<String,ResourceSummary> result = new LinkedHashMap();
        for (ResourceSummary rs : resources) {
            result.put(rs.getResource(), rs);
        }
        
        if(!resources.isEmpty()) {
            monitor.query(query(), new RequestDataVisitor() {
                public void visit(RequestData data, Object... aggregates) {
                    String resource = data.getResources().get(0);
                    String operation = data.getOperation();
                    Long count = ((Number)aggregates[0]).longValue();
                    
                    ResourceSummary summary = result.get(resource);
                    RequestSummary request = new RequestSummary();
                    request.setRequest(operation);
                    request.setCount(count);
                    if (summary.getCount() != null) {
                        request.setPercent(count / ((double)summary.getCount()));
                    }
                    
                    summary.getRequests().add(request);
                }
            });
        }
        return new ArrayList(result.values());
    }

}
