package org.opengeo.analytics;

import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;

public class CountingVisitor implements RequestDataVisitor {

    long count = 0;
    
    public void visit(RequestData data, Object... aggregates) {
        count++;
    }
    
    public long getCount() {
        return count;
    }
}
