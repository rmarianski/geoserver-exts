package org.opengeo.analytics;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.geoserver.monitor.Query;
import org.junit.Test;

public class TimeAggregatorTest extends AnalyticsTestSupport {

    @Test
    public void testHour() throws Exception {
        Date from = testData.toDate("2010-07-23T16:00:44");
        Date to = testData.toDate("2010-07-23T17:00:00");
        
        TimeAggregator agg = new TimeAggregator(from, to, View.HOURLY);
        
        Query q = new Query().between(from, to);
        monitor.query(q, agg);
        
        long[] data = agg.getData();
        assertEquals(61, data.length);
      
        
        assertEquals(0, data[1]); 
        assertEquals(0, data[59]); 
        assertEquals(1, data[6]); //"2010-07-23T16:06:44"
        assertEquals(1, data[16]); //"2010-07-23T16:16:44"
        assertEquals(1, data[26]); //"2010-07-23T16:26:44"
        assertEquals(1, data[36]); //"2010-07-23T16:36:44" 
        assertEquals(1, data[46]); //"2010-07-23T16:46:44"
        assertEquals(1, data[56]); //"2010-07-23T16:56:44
    }
    
    @Test
    public void testDay() throws Exception {
        Date from = testData.toDate("2010-08-23T15:26:44");
        Date to = testData.toDate("2010-08-24T00:00:00");
        
        TimeAggregator agg = new TimeAggregator(from, to, View.DAILY);
        
        Query q = new Query().between(from, to);
        monitor.query(q, agg);
        
        long[] data = agg.getData();
        assertEquals(10, data.length);
        
        //"2010-08-23T15:26:59"
        //"2010-08-23T15:36:47"
        //"2010-08-23T15:46:52"
        //"2010-08-23T15:56:48"
        assertEquals(4, data[0]);
        
        //"2010-08-23T16:06:45"
        //"2010-08-23T16:16:53"
        //"2010-08-23T16:26:47"
        //"2010-08-23T16:36:46"
        //"2010-08-23T16:46:53"
        //"2010-08-23T16:56:44"
        assertEquals(6, data[1]);
        
        //none
        assertEquals(0, data[2]);
    }
    
    @Test
    public void testWeek() throws Exception {
        Date from = testData.toDate("2010-08-29T15:00:00");
        Date to = testData.toDate("2010-09-10T17:00:00");
        
        TimeAggregator agg = new TimeAggregator(from, to, View.WEEKLY);
        
        Query q = new Query().between(from, to);
        monitor.query(q, agg);
        
        long[] data = agg.getData();
        assertEquals(13, data.length);
        
        assertEquals(1, data[0]); //"2010-08-29T16:16:44"
        assertEquals(1, data[6]); //"2010-09-04T16:26:44"
        assertEquals(1, data[11]);//"2010-09-09T16:36:44",
        assertEquals(1, data[12]); //"2010-09-10T16:46:44"
    }
}
