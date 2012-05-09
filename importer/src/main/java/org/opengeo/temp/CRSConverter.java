package org.opengeo.temp;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

/**
 * Converter for coordinate reference system objects that converts by WKT.
 */
public class CRSConverter extends AbstractSingleValueConverter {

    @Override
    public boolean canConvert(Class type) {
        return CoordinateReferenceSystem.class.isAssignableFrom(type);
    }

    @Override
    public String toString(Object obj) {
        return ((CoordinateReferenceSystem) obj).toWKT();
    }

    @Override
    public Object fromString(String str) {
        try {
            return CRS.parseWKT(str);
        } catch (Exception e) {
            try {
                return new SRSConverter().fromString(str);
            } catch (Exception e1) {
            }

            throw new RuntimeException(e);
        }
    }

}
