package org.opengeo.analytics;

import java.util.Date;

import org.geoserver.monitor.RequestData;

/**
 * Aggregates requests by start time.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class TimeAggregator extends RequestDataAggregator {

    View view;
    long[] data;
    
    public TimeAggregator(Date from, Date to, View view) {
        super(from, to);
        this.view = view;
        this.data = new long[(int) (view.period().diff(from, to) + 1)];
    }
    
    public void visit(RequestData r, Object... aggregates) {
        int index = index(r.getStartTime());
        data[index]++;
    }
    
    int index(Date time) {
        return (int) view.period().diff(from, time);
    }

    public long[] getData() {
        return data;
    }
}
