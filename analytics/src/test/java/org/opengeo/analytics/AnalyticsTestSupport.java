package org.opengeo.analytics;

import java.text.ParseException;
import java.util.List;

import org.geoserver.monitor.MemoryMonitorDAO;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorTestData;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.junit.Before;
import org.junit.Test;

import static org.geoserver.monitor.MonitorTestData.toDate;
import static org.junit.Assert.assertEquals;

public class AnalyticsTestSupport {

    protected Monitor monitor;
    protected MonitorTestData testData;
    
    @Before
    public void setup() throws Exception {
        MemoryMonitorDAO dao = new MemoryMonitorDAO();
        monitor = new Monitor(dao);
        
        testData = new MonitorTestData(dao, true) {
            protected void addTestData(List<RequestData> datas) throws ParseException {
                datas.add(data(21, "/earth", "2010-08-24T16:16:44", "2010-08-24T16:16:53", "WAITING", 
                        "earth", "x", "stuff"));
                datas.add(data(22, "/saturn", "2010-08-24T16:26:44", "2010-08-24T16:26:47", "FINISHED", 
                        "saturn", "z", "things", "stuff"));
                datas.add(data(23, "/venus", "2010-08-24T16:36:44", "2010-08-24T16:36:46", "FAILED", 
                        "venus", "y", "widgets"));
                datas.add(data(24, "/uranus", "2010-08-24T16:46:44", "2010-08-24T16:46:53", "CANCELLING", 
                        "uranus", "y", "stuff"));
                datas.add(data(25, "/earth", "2010-08-24T16:56:44", "2010-08-24T16:56:47", "RUNNING", 
                        "earth", "x", "things"));
                datas.add(data(26, "/mercury", "2010-08-29T16:16:44", "2010-08-29T16:16:53", "WAITING", 
                        "mercury", "x", "stuff"));
                datas.add(data(27, "/pluto", "2010-09-04T16:26:44", "2010-09-04T16:26:47", "FINISHED", 
                        "pluto", "z", "things", "stuff"));
                datas.add(data(28, "/neptune", "2010-09-09T16:36:44", "2010-09-09T16:36:46", "FAILED", 
                        "neptune", "y", "widgets"));
                datas.add(data(29, "/jupiter", "2010-09-10T16:46:44", "2010-09-10T16:46:53", "CANCELLING", 
                        "jupiter", "y", "stuff"));
                
            }
        };
        testData.setup();
    }
    
    @Test
    public void testSanity() throws Exception {
        Query q = new Query().between(toDate("2010-07-23T15:56:44"), toDate("2010-08-23T16:06:44"));
        assertEquals(12, monitor.getDAO().getRequests(q).size());
    }

}
