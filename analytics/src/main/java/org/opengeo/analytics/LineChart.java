package org.opengeo.analytics;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.Map;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import java.util.Arrays;

public class LineChart extends Chart {

    protected int steps;
    protected View zoom;
    protected Date from, to;
    protected Map<String,long[]> data;
    private long requestTotal;
    private long queryTotal;
    private long[] failed;
    
    public View getZoom() {
        return zoom;
    }

    public void setZoom(View zoom) {
        this.zoom = zoom;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }
    
    public void setFrom(Date from) {
        this.from = from;
    }
    
    public Date getFrom() {
        return from;
    }
    
    public Map<String, long[]> getData() {
        return data;
    }
    
    public void setData(Map<String, long[]> data) {
        this.data = data;
    }
    
    public void setTo(Date to) {
        this.to = to;
    }
    
    public Date getTo() {
        return to;
    }
    
    public void setRequestTotal(long requestTotal) {
        this.requestTotal = requestTotal;
    }
    
    public void setQueryTotal(long queryTotal) {
        this.queryTotal = queryTotal;
    }
    
    public void setFailed(long[] failed) {
        this.failed = failed;
    }
    
    private boolean buildSeries(long[] xy,StringBuilder x,StringBuilder y,boolean first) {
        y.append("[");
        if (first) {
            for (int i = 0; i < xy.length; i++) {
                x.append(i).append(",");
                y.append(xy[i]).append(",");
            }
        }
        else {
            for (int i = 0; i < xy.length; i++) {
                y.append(xy[i]).append(",");
            }
        }

        y.setLength(y.length()-1);
        y.append("],");
        return false;
    }
    
    public void render(Writer writer) 
        throws IOException, TemplateException {
        
        //build up the xy data
        StringBuilder x = new StringBuilder();
        StringBuilder y = new StringBuilder();
        StringBuilder c = new StringBuilder();
        
        x.append("["); y.append("["); c.append("[");
        boolean first = true;
        for (Map.Entry<String, long[]> e : data.entrySet()) {
            c.append("'").append(Service.valueOf(e.getKey()).color()).append("',");
            first = buildSeries(e.getValue(), x, y, first);
        }
        if (!data.isEmpty() && failed != null) {
            c.append("'").append("red").append("',");
            first = buildSeries(failed, x, y, first);
        }
        
        if (x.length() > 1) {
            x.setLength(x.length()-1);
            y.setLength(y.length()-1);
            c.setLength(c.length()-1);
        }
        x.append("]");
        y.append("]");
        c.append("]");

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
        first = true;
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
        breaks.append("]");
        
        SimpleHash model = createTemplate();
        model.put("xdata", x.toString());
        model.put("xlen", !data.isEmpty() ? data.values().iterator().next().length : -1);
        model.put("ydata", y.toString());
        model.put("labels", labels.toString());
        model.put("colors", c.toString());
        model.put("xsteps", steps);
        model.put("breaks", breaks);
        model.put("ylabel", (queryTotal == 0 ? "No Data" : "#Requests") + " [ " + queryTotal + " of " + requestTotal + " ]");
        
        render(model, "line.ftl", writer);
    }
}
