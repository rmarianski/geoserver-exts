package org.opengeo.analytics;

import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;

import org.geoserver.monitor.Query;
import org.junit.Before;
import org.junit.Test;
import static org.geoserver.monitor.MonitorTestData.toDate;
public class LineChartTest extends AnalyticsTestSupport {

    LineChart chart;
    
    @Before
    public void setupChart() {
        chart = new LineChart();
        
        chart.setHeight(300);
        chart.setWidth(500);
        chart.setContainer("chart");
        chart.setSteps(15);
    }
    
    @Test
    public void testLineHourly() throws Exception {
        chart.setZoom(View.HOURLY);
        
        Date from = toDate("2010-08-24T16:00:00");
        Date to = toDate("2010-08-24T17:00:00");
        //Date from = toDate("2010-08-23T00:00:00");
        //Date to = toDate("2010-08-25T00:00:00");
        
        TimeAggregator agg = new TimeAggregator(from, to, View.HOURLY);
        monitor.query(new Query().between(from,to), agg);
        
        chart.setFrom(from);
        chart.setTo(to);
        
        HashMap map = new HashMap();
        map.put("OTHER", agg.getData());
        chart.setData(map);
        
        StringWriter writer = new StringWriter();
        chart.render(writer);
        System.out.println(writer.toString());
    }
    
    @Test
    public void testLineDaily() throws Exception {
        chart.setZoom(View.DAILY);
        
        Date from = toDate("2010-08-23T00:00:00");
        Date to = toDate("2010-08-25T00:00:00");
        
        TimeAggregator agg = new TimeAggregator(from, to, View.HOURLY);
        monitor.query(new Query().between(from,to), agg);
        
        chart.setFrom(from);
        chart.setTo(to);
        
        HashMap map = new HashMap();
        map.put("OTHER", agg.getData());
        chart.setData(map);
        
        StringWriter writer = new StringWriter();
        chart.render(writer);
        //System.out.println(writer.toString());
    }
    
}
