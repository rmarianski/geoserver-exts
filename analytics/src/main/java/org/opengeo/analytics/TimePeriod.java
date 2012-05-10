package org.opengeo.analytics;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Weeks;

public enum TimePeriod {

    MINUTE {
        @Override
        DateTime floor(DateTime dt) {
            return dt.minuteOfDay().roundFloorCopy();
        }
        @Override
        DateTime ceil(DateTime dt) {
            return dt.minuteOfDay().roundCeilingCopy();
        }
        
        @Override
        long diff(DateTime t1, DateTime t2) {
            return Minutes.minutesBetween(t1, t2).getMinutes();
        }
        
        @Override
        DateTime plus(DateTime t, int amt) {
            return t.plusMinutes(amt);
        }
        
        @Override
        DateTime minus(DateTime t, int amt) {
            return t.minusMinutes(amt);
        }
        
        @Override
        public TimePeriod down() { return null; }

        @Override
        public TimePeriod up() { return HOUR; }
    }, 
    
    HOUR {
        @Override
        DateTime floor(DateTime dt) {
            return dt.hourOfDay().roundFloorCopy();
        }
        
        @Override
        DateTime ceil(DateTime dt) {
            return dt.hourOfDay().roundCeilingCopy();
        }
        
        @Override
        long diff(DateTime t1, DateTime t2) {
            return Hours.hoursBetween(t1, t2).getHours();
        }
        
        @Override
        DateTime plus(DateTime t, int amt) {
            return t.plusHours(amt);
        }
        
        @Override
        DateTime minus(DateTime t, int amt) {
            return t.minusHours(amt);
        }
        
        @Override
        public TimePeriod down() { return MINUTE; }

        @Override
        public TimePeriod up() { return DAY; }
    },
    
    DAY {
        @Override
        DateTime floor(DateTime dt) {
            return dt.dayOfMonth().roundFloorCopy();
        }
        
        @Override
        DateTime ceil(DateTime dt) {
            return dt.dayOfMonth().roundCeilingCopy();
        }
        
        @Override
        long diff(DateTime t1, DateTime t2) {
            return Days.daysBetween(t1, t2).getDays();
        }
        
        @Override
        DateTime plus(DateTime t, int amt) {
            return t.plusDays(amt);
        }
        
        @Override
        DateTime minus(DateTime t, int amt) {
            return t.minusDays(amt);
        }
        
        @Override
        public TimePeriod down() { return HOUR; }

        @Override
        public TimePeriod up() { return WEEK; }
    }, 
    
    WEEK {
        @Override
        DateTime floor(DateTime dt) {
            return dt.weekOfWeekyear().roundFloorCopy();
        }
        
        @Override
        DateTime ceil(DateTime dt) {
            return dt.weekOfWeekyear().roundCeilingCopy();
        }
        
        @Override
        long diff(DateTime t1, DateTime t2) {
            return Weeks.weeksBetween(t1, t2).getWeeks();
        }
        
        @Override
        DateTime plus(DateTime t, int amt) {
            return t.plusWeeks(amt);
        }
        
        @Override
        DateTime minus(DateTime t, int amt) {
            return t.minusWeeks(amt);
        }
        
        @Override
        public TimePeriod down() { return DAY; }

        @Override
        public TimePeriod up() { return MONTH; }
    }, 
    
    MONTH {
        @Override
        DateTime floor(DateTime dt) {
            return dt.monthOfYear().roundFloorCopy();
        }
        
        @Override
        DateTime ceil(DateTime dt) {
            return dt.monthOfYear().roundCeilingCopy();
        }
        
        @Override
        long diff(DateTime t1, DateTime t2) {
            return Months.monthsBetween(t1, t2).getMonths();
        }
        
        @Override
        DateTime plus(DateTime t, int amt) {
            return t.plusMonths(amt);
        }
        
        @Override
        DateTime minus(DateTime t, int amt) {
            return t.minusMonths(amt);
        }
        
        @Override
        public TimePeriod down() { return WEEK; }

        @Override
        public TimePeriod up() { return null; }
    };
    
    public abstract TimePeriod up();
    public abstract TimePeriod down();
    
    abstract DateTime floor(DateTime t);
    abstract DateTime ceil(DateTime t);
    abstract DateTime plus(DateTime t, int amt);
    abstract DateTime minus(DateTime t, int amt);
    
    abstract long diff(DateTime t1, DateTime t2);
    
    public Date getInitalRange(Date now) {
        DateTime dt = new DateTime(now);
        dt = floor(dt);
        //buffer(c);
        return dt.toDate();
    }
    
    public Date floor(Date now) {
        DateTime dt = new DateTime(now);
        dt = floor(dt);
        return dt.toDate();
    }
    
    public Date ceil(Date now) {
        DateTime dt = new DateTime(now);
        dt = ceil(dt);
        return dt.toDate();
    }
    
    public long diff(Date d1, Date d2) {
        DateTime dt1 = new DateTime(d1);
        dt1 = floor(dt1);
        
        DateTime dt2 = new DateTime(d2);
        dt2 = floor(dt2);
        
        return diff(dt1, dt2);
    }
    
    public Date minus(Date d, int n) {
        return minus(new DateTime(d), n).toDate();
    }
    
    public Date plus(Date d, int n) {
        return plus(new DateTime(d), n).toDate();
    }
    
    public List<Date> divide(Date d1, Date d2, int n) {
        if (d1.after(d2)) {
            throw new IllegalArgumentException(d1 + " is after " + d2);
        }
        
        DateTime dt1 = new DateTime(d1);
        DateTime dt2 = new DateTime(d2);
     
        List<Date> dates = new ArrayList<Date>();
        int diff = (int) Math.max(Math.round(diff(dt1, dt2) / ((double)n)), 1);
        do {
            dates.add(dt1.toDate());
            dt1 = plus(dt1, diff);
        }
        while(diff > 0 && !dt1.isAfter(dt2));
        
        if (dates.get(dates.size()-1).before(d2)) {
            dates.add(dt1.toDate());
        }
        return dates;
    }
    
    public List<Date> divide(Date d1, Date d2) {
        DateTime dt1 = new DateTime(d1);
        DateTime dt2 = new DateTime(d2);
     
        List<Date> dates = new ArrayList<Date>();
        do {
            dates.add(dt1.toDate());
            dt1 = plus(dt1, 1);
        }
        while(!dt1.isAfter(dt2));
        
        return dates;
    }
}
