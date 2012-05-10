package org.opengeo.analytics;

import java.util.Date;

import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;

/**
 * Aggregates requests into discrete time units.
 *  
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class RequestDataAggregator implements RequestDataVisitor {

    protected Date from, to;
    protected int div;
     
    protected RequestDataAggregator(Date from, Date to) {
        this.from = from;
        this.to = to;
    }
    
    public Date getFrom() {
        return from;
    }
    
    public Date getTo() {
        return to;
    }
}
