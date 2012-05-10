package org.opengeo.analytics;

import java.util.Date;

import org.joda.time.DateTime;

public enum View {
    HOURLY {
        @Override
        public TimePeriod period() { return TimePeriod.MINUTE; };
        
        @Override
        public String label(DateTime d) {
            return d.toString("kk:mm");
        };
        
        @Override
        public Date initialRange(Date now) {
            return TimePeriod.HOUR.minus(TimePeriod.HOUR.floor(now), 2);
        }
        
        @Override
        public Date minimumRange(Date then, Date now) {
            if (TimePeriod.MINUTE.diff(then, now) < 1) {
                return TimePeriod.MINUTE.floor(TimePeriod.MINUTE.minus(then, 1));
            }
            return then;
        }
    },
    
    DAILY {
        @Override
        public TimePeriod period() { return TimePeriod.HOUR; };
        
        @Override
        public String label(DateTime d) {
            return d.toString("kk:mm");
        }
        
        @Override
        public Date initialRange(Date now) {
            return TimePeriod.HOUR.floor(TimePeriod.HOUR.minus(now, 24));
            //return TimePeriod.DAY.floor(now);
        }
        
        @Override
        public Date minimumRange(Date then, Date now) {
            if (TimePeriod.HOUR.diff(then, now) < 1) {
                return TimePeriod.HOUR.floor(TimePeriod.HOUR.minus(then, 1));
            }
            return then;
        }
    },
    
    WEEKLY {
        @Override
        public TimePeriod period() { return TimePeriod.DAY; };
        
        @Override
        public String label(DateTime d) {
            return d.toString("E");
        }
        
        @Override
        public Date initialRange(Date now) {
            return TimePeriod.WEEK.floor(now);
        }
        
        @Override
        public Date minimumRange(Date then, Date now) {
            if (TimePeriod.WEEK.diff(then, now) < 1) {
                return TimePeriod.WEEK.floor(TimePeriod.WEEK.minus(then, 1));
            }
            return then;
        }
    },
    
    MONTHLY {
        @Override
        public TimePeriod period() { return TimePeriod.WEEK; };
        
        @Override
        public String label(DateTime d) {
            return d.toString("MMM dd");
        };
        
        @Override
        public Date initialRange(Date now) {
            return TimePeriod.MONTH.floor(now);
        }
        
        @Override
        public Date minimumRange(Date then, Date now) {
            if (TimePeriod.MONTH.diff(then, now) < 1) {
                return TimePeriod.MONTH.floor(TimePeriod.MONTH.minus(then, 1));
            }
            return then;
        }
    };
    
    public String label(Date d) {
        return label(new DateTime(d));
    }
    
    public Date minimumRange(Date then, Date now) {
        return then;
    }
    
    public abstract TimePeriod period();
    public abstract Date initialRange(Date now);
    
    abstract String label(DateTime dt);
}