package org.opengeo.analytics;

import static org.geoserver.monitor.MonitorTestData.toDate;
import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

public class TimePeriodTest {

    @Test
    public void testHour() {
        Date from = toDate("2010-07-23T16:30:44");
        Date to = toDate("2010-07-23T17:00:00");
        
        assertEquals(1, TimePeriod.HOUR.diff(from, to));
        
        from = toDate("2010-07-23T16:30:44");
        to = toDate("2010-07-24T03:00:00");
        assertEquals(11, TimePeriod.HOUR.diff(from, to));

        from = toDate("2010-07-23T16:30:44");
        to = toDate("2010-08-24T03:00:00");
        assertEquals(755, TimePeriod.HOUR.diff(from, to));
        
        from = toDate("2010-08-23T16:30:44");
        to = toDate("2010-08-23T16:45:00");
        assertEquals(0, TimePeriod.HOUR.diff(from, to));
    }
    
    @Test
    public void testDay() {
        Date from = toDate("2010-07-23T16:30:44");
        Date to = toDate("2010-07-23T17:00:00");
        assertEquals(0, TimePeriod.DAY.diff(from, to));
        
        from = toDate("2010-07-23T16:30:44");
        to = toDate("2010-07-24T09:00:00");
        assertEquals(1, TimePeriod.DAY.diff(from, to));

        from = toDate("2010-07-23T16:30:44");
        to = toDate("2010-08-24T09:00:00");
        assertEquals(32, TimePeriod.DAY.diff(from, to));
    }
    
    @Test
    public void testWeek() {
        Date from = toDate("2010-07-05T16:30:44");
        Date to = toDate("2010-07-10T17:00:00");
        assertEquals(0, TimePeriod.WEEK.diff(from, to));
        
        from = toDate("2010-07-23T16:30:44");
        to = toDate("2010-07-26T09:00:00");
        assertEquals(1, TimePeriod.WEEK.diff(from, to));

        from = toDate("2010-07-23T16:30:44");
        to = toDate("2010-08-24T09:00:00");
        assertEquals(5, TimePeriod.WEEK.diff(from, to));
    }
    
    @Test
    public void testMonth() {
        Date from = toDate("2010-07-05T16:30:44");
        Date to = toDate("2010-07-10T17:00:00");
        assertEquals(0, TimePeriod.MONTH.diff(from, to));
        
        from = toDate("2010-07-23T16:30:44");
        to = toDate("2010-08-26T09:00:00");
        assertEquals(1, TimePeriod.MONTH.diff(from, to));

        from = toDate("2010-07-23T16:30:44");
        to = toDate("2011-08-24T09:00:00");
        assertEquals(13, TimePeriod.MONTH.diff(from, to));
    }
}
