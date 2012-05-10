package org.opengeo.analytics;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;

public class PerformanceLineChart extends Chart {

    protected int steps;
    protected View zoom;
    protected Date from, to;
    double[] timeData, thruData;

    public void setFrom(Date from) {
        this.from = from;
    }
   
    public Date getFrom() {
        return from;
    }
    
    public void setTo(Date to) {
        this.to = to;
    }
    
    public Date getTo() {
        return to;
    }
    
    public void setZoom(View zoom) {
        this.zoom = zoom;
    }
    
    public View getZoom() {
        return zoom;
    }
    
    public void setSteps(int steps) {
        this.steps = steps;
    }
    
    public int getSteps() {
        return steps;
    }
    
    public void setTimeData(double[] data) {
        this.timeData = data;
    }
    
    public void setThroughputData(double[] data) {
        this.thruData = data;
    }
    
    @Override
    public void render(Writer writer) throws IOException, TemplateException {
        //build up the xy data
        StringBuilder x = new StringBuilder();
        StringBuilder time = new StringBuilder();
        StringBuilder thru = new StringBuilder();
        
        x.append("["); time.append("["); thru.append("[");
        for (int i = 0; i < timeData.length; i++) {
            x.append(i).append(",");
            time.append(timeData[i]).append(",");
            thru.append(thruData[i]).append(",");
        }
        
        x.setLength(x.length()-1);
        x.append("]");
        
        time.setLength(time.length()-1);
        time.append("]");
        
        thru.setLength(thru.length()-1);
        thru.append("]");
        
        //build the labels
        List<Date> dates = zoom.period().divide(from, to, steps);
        int steps = dates.size()-1;
        
        StringBuilder labels = new StringBuilder().append("[");
        for (Date d : dates) {
            labels.append("'").append(zoom.label(d)).append("',");
        }
        labels.setLength(labels.length()-1);
        labels.append("]");
        
        //figure out where the breaks occur at the higher zoom
        TimePeriod up;
        switch(zoom) {
            case HOURLY:
            case DAILY:
                up = TimePeriod.DAY;
                break;
            case WEEKLY:
                up = TimePeriod.WEEK;
                break;
                
            default:
                up = null;
        }
        
        StringBuilder breaks = new StringBuilder("[");
        boolean first = true;
        if (up != null) {
            int dx = (int) zoom.period().diff(dates.get(0), dates.get(1));
            for (int i = 0; i < dates.size()-1; i++) {
                Date d1 = dates.get(i);
                Date d2 = dates.get(i+1);
                
                //figure out if a boundary is crossed at the higher zoom
                if (up.diff(d1, d2) > 0) {
                    int diff = (int) zoom.period().diff(d1, up.floor(d2));
                    if (!first) {
                        breaks.append(',');
                    }
                    first = false;
                    breaks.append('[');
                    breaks.append(i).append(',');
                    breaks.append(diff).append(',');
                    breaks.append(dx).append(',');
                    breaks.append("'").append(View.MONTHLY.label(up.floor(d2))).append("'");
                    breaks.append(']');
                }
            }
        }
        breaks.append(']');
        
        SimpleHash model = createTemplate();
        model.put("xdata", x.toString());
        model.put("xlen",  timeData.length != 0 ? timeData.length : -1 );
        model.put("timeData", time.toString());
        model.put("thruData", thru.toString());
        model.put("labels", labels.toString());
        model.put("xsteps", steps);
        model.put("breaks", breaks.toString());
        
        render(model, "performance-line.ftl", writer);
    }

}
