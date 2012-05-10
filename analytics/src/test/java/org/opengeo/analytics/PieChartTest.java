package org.opengeo.analytics;

import java.io.StringWriter;
import java.util.Map;

import org.geoserver.monitor.Query;
import org.junit.Before;
import org.junit.Test;

public class PieChartTest extends AnalyticsTestSupport {

    PieChart chart;
    
    @Before
    public void setupChart() {
        chart = new PieChart();
        
        chart.setHeight(300);
        chart.setWidth(500);
        chart.setContainer("chart");
    }
    
    @Test
    public void test() throws Exception {
        ServiceAggregator agg = new ServiceAggregator();
        monitor.query(new Query(), agg);
        
        chart.setData(agg.getData());
        
        StringWriter writer = new StringWriter();
        chart.render(writer);
        System.out.println(writer.toString());
    }


}
