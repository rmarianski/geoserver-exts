package org.opengeo.analytics;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.Map;

import org.geoserver.monitor.Query;
import org.junit.Test;

public class ServiceTimeAggregatorTest extends AnalyticsTestSupport {

    @Test
    public void testHour() throws Exception {
        
        Date from = testData.toDate("2010-08-23T15:00:00");
        Date to = testData.toDate("2010-08-23T16:57:00");
        
        ServiceTimeAggregator agg = 
            new ServiceTimeAggregator(new Query().between(from, to), View.HOURLY);
        
        Query q = new Query().between(from, to);
        monitor.query(q, agg);
        
        Map<String,long[]> map = agg.getData();
        assertEquals(4, map.size());
      
        //"2010-08-23T15:26:44", "foo"
        //"2010-08-23T16:06:44", "foo"
        //"2010-08-23T16:16:44", "foo"
        //"2010-08-23T16:56:44", "foo"
        long[] data = map.get("foo");
        assertEquals(1, data[26]);
        assertEquals(1, data[66]);
        assertEquals(0, data[67]);
        assertEquals(1, data[76]);
        assertEquals(1, data[116]);
        assertEquals(0, data[117]);
        
        //"2010-08-23T15:36:44", "bar"
        //"2010-08-23T16:26:44", "bar"
        data = map.get("bar");
        assertEquals(1, data[36]);
        assertEquals(1, data[86]);
        assertEquals(0, data[87]);
        
        //"2010-08-23T15:46:44", "baz"
        data = map.get("baz");
        assertEquals(1, data[46]);
        assertEquals(0, data[86]);
        
        //"2010-08-23T15:56:44", "bam"
        //"2010-08-23T16:36:44", "bam"
        //"2010-08-23T16:46:44", "bam"
        data = map.get("bam");
        assertEquals(1, data[56]);
        assertEquals(1, data[96]);
        assertEquals(1, data[106]);
        assertEquals(0, data[86]);
    }
}
