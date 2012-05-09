package org.opengeo.temp;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

/**
 * Converter for coordinate reference system objects that converts by SRS code.
 */
public class SRSConverter extends AbstractSingleValueConverter {

    static Logger LOGGER = Logging.getLogger("org.geoserver");

    public boolean canConvert(Class type) {
        return CoordinateReferenceSystem.class.isAssignableFrom(type);
    }

    @Override
    public String toString(Object obj) {
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) obj;
        try {
            Integer epsg = CRS.lookupEpsgCode(crs, true);
            if (epsg != null) {
                return "EPSG:" + epsg;
            }
        } catch (FactoryException e) {
            LOGGER.warning("Could not determine epsg code of crs, encoding as WKT");
        }

        return crs.toWKT();
    }

    @Override
    public Object fromString(String str) {
        if (str.toUpperCase().startsWith("EPSG:")) {
            try {
                return CRS.decode(str);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error decode epsg code: " + str, e);
            }
        } else {
            try {
                return CRS.parseWKT(str);
            } catch (FactoryException e) {
                LOGGER.log(Level.WARNING, "Error decode wkt: " + str, e);
            }
        }
        return null;
    }
}
