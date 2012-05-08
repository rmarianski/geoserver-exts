package org.opengeo.data.importer.transform;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;

import org.geotools.data.DataStore;
import org.opengeo.data.importer.ImportItem;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Transform that converts a non date attribute in a date attribute.
 * This class is not thread-safe.
 *
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class DateFormatTransform extends AttributeRemapTransform {
    
    private static final long serialVersionUID = 1L;
    /**
     * All patterns that are correct regarding the ISO-8601 norm.
     */
    static final String[] PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:sss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm'Z'",
        "yyyy-MM-dd'T'HH'Z'",
        "yyyy-MM-dd",
        "yyyy-MM",
        "yyyy"
    };
    static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");
    transient ParsePosition pos;
    SimpleDateFormat dateFormat;
    transient SimpleDateFormat lastFormat;
    transient SimpleDateFormat[] patterns;

    public DateFormatTransform(String field, String datePattern) {
        init(field,datePattern);
        init();
    }
    
    DateFormatTransform() {
        this(null,null);
    }
    
    private void init(String field, String datePattern) {
        setType(Date.class);
        setField(field);
        if (datePattern != null) {
            // @todo allow timezone?
            this.dateFormat = new SimpleDateFormat(datePattern);
            this.dateFormat.setTimeZone(UTC_TZ);
        }
    }
    
    public final void init() {
        pos = new ParsePosition(0);
        patterns = new SimpleDateFormat[PATTERNS.length];
        for (int i = 0; i < PATTERNS.length; i++) {
            SimpleDateFormat format = new SimpleDateFormat(PATTERNS[i], Locale.CANADA);
            format.setTimeZone(UTC_TZ);
            patterns[i] = format;
        }
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }
    
    @Override
    public SimpleFeature apply(ImportItem item, DataStore dataStore, SimpleFeature oldFeature,
            SimpleFeature feature) throws Exception {
        Object val = feature.getAttribute(field);
        if (val != null) {
            Date parsed = parseDate(val.toString());
            if (parsed == null) {
                item.addImportMessage(Level.WARNING, "Invalid date '" + val + "' specified for " + feature.getID());
                feature = null;
            } else {
                feature.setAttribute(field, parsed);
            }
        }
        return feature;
    }

    private Date parseDate(SimpleDateFormat format, String value) throws ParseException {
        Date parsed = null;
        /*
         * We do not use the standard method DateFormat.parse(String), because
         * if the parsing stops before the end of the string, the remaining
         * characters are just ignored and no exception is thrown. So we have to
         * ensure that the whole string is correct for the format.
         */
        pos.setIndex(0);
        Date p = format.parse(value, pos);
        if (pos.getIndex() == value.length()) {
            parsed = p;
            lastFormat = format;
        }
        return parsed;
    }

    Date parseDate(String value) throws ParseException {
        Date parsed = null;
        
        // if a format was provided, use it
        if (dateFormat != null) {
            parsed = parseDate(dateFormat, value);
        }

        // fall back to others
        if (parsed == null) {
            // optimization to use last working pattern
            if (lastFormat != null) {
                parsed = parseDate(lastFormat, value);
            }
            for (int i = 0; i < patterns.length && parsed == null; i++) {
                parsed = parseDate(patterns[i], value);
            }
        }
        if (parsed != null) {
            return parsed;
        }

        throw new ParseException("Invalid date '" + value + "'", 0);
    }
}
