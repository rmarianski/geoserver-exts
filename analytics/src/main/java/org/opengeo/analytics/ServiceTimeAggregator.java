package org.opengeo.analytics;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;

/**
 * Aggregats request data grouped by service over time.
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ServiceTimeAggregator extends RequestDataAggregator {

    Query query;
    View view;
    Set<String> services;
    Map<String,TimeAggregator> data = new HashMap();
    long[] failed;
    
    public ServiceTimeAggregator(Query query, View view) {
        this(query, view, new HashSet());
    }
    
    public ServiceTimeAggregator(Query query, View view, Set<String> services) {
        super(query.getFromDate(), query.getToDate());
        
        this.query = query.clone();
        this.query.getProperties().clear();
        this.query.getAggregates().clear();
        this.query.getGroupBy().clear();
        this.query.properties("service", "startTime","status");
        this.failed = new long[(int) (view.period().diff(query.getFromDate(), query.getToDate()) + 1)];
        
        this.view = view;
        this.services = services;
    }

    public Query getQuery() {
        return query;
    }
    
    public void visit(RequestData req, Object... aggregates) {
        String service = req.getService();
        service = service != null ? service : Service.OTHER.name();
        
        if (!services.isEmpty() && !services.contains(service)) {
            return;
        }
        
        if (req.getStatus() == RequestData.Status.FAILED) {
            int fidx = (int) view.period().diff(query.getFromDate(), req.getStartTime());
            failed[fidx]+=1;
        }
        
        TimeAggregator del = data.get(service);
        if (del == null) {
            del = new TimeAggregator(from, to, view);
            data.put(service, del);
        }
        
        del.visit(req, aggregates);
    }
    
    public long[] getTotalFailed() {
        return failed;
    }
    
    public Map<String,long[]> getData() {
        Map<String,long[]> result = new HashMap();
        
        for (Map.Entry e : data.entrySet()) {
            result.put((String)e.getKey(), ((TimeAggregator)e.getValue()).getData());
        }
        
        return result;
    }
    
    public long[][] getRawData() {
        long[][] result = new long[data.size()][];
        int i = 0;
        for (Map.Entry e : data.entrySet()) {
            result[i++] = ((TimeAggregator)e.getValue()).getData();
        }
        return result;
    }

}
