package org.opengeo.data.importer.mosaic;

import java.util.Date;

import org.opengeo.data.importer.Dates;

/**
 * Enumeration for handling timestamps for granules. 
 *   
 * @author Justin Deoliveira, OpenGeo
 */
public enum TimeMode {
    /**
     * Extract the timestamp from the filename, via {@link FilenameTimeHandler} 
     */
    FILENAME {
        @Override
        public FilenameTimeHandler createHandler() {
            return new FilenameTimeHandler();
        }
    }, 

    /**
     * Timestamps handled manualy, and input directly by the user. 
     */
    MANUAL {
        @Override
        public TimeHandler createHandler() {
            return new TimeHandler() {
                @Override
                public Date computeTimestamp(Granule g) {
                    return g.getTimestamp();
                }
            };
        }
    },

    AUTO {
        @Override
        public TimeHandler createHandler() {
            return new TimeHandler() {
                @Override
                public Date computeTimestamp(Granule g) {
                    return Dates.matchAndParse(g.getFile().getName());
                }
            };
        }
    }, 
    
    /**
     * No timestamp handling.
     */
    NONE {
        @Override
        public TimeHandler createHandler() {
            return new TimeHandler() {
                @Override
                public Date computeTimestamp(Granule g) {
                    return null;
                }
            };
        }
    };

    public abstract TimeHandler createHandler();
}