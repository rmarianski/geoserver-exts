package org.opengeo.analytics;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;

public class ServiceAggregator implements RequestDataVisitor {

    HashMap<String,ServiceOpSummary> data = new HashMap();
    
    public void visit(RequestData req, Object... aggregates) {
        String service = req.getService();
        service = service != null ? service : Service.OTHER.name();
        ServiceOpSummary summary = data.get(service);
        if (summary == null) {
            summary = new ServiceOpSummary(service);
            data.put(service, summary);
        }
        
        summary.add(req.getOperation(), 1);
    }
    
    public Map<String,ServiceOpSummary> getData() {
        return data;
    }

}
