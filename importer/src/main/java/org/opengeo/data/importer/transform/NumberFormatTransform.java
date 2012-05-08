package org.opengeo.data.importer.transform;

import org.geotools.data.DataStore;
import org.geotools.util.Converters;
import org.opengeo.data.importer.ImportItem;
import org.opengis.feature.simple.SimpleFeature;

public class NumberFormatTransform extends AttributeRemapTransform {

    public NumberFormatTransform(String field, Class<? extends Number> type) {
        super(field, type);
    }

    @Override
    public SimpleFeature apply(ImportItem item, DataStore dataStore, SimpleFeature oldFeature, 
        SimpleFeature feature) throws Exception {
        Object val = feature.getAttribute(field);
        if (val != null) {
            feature.setAttribute(field, Converters.convert(val, type));
        }
        return feature;
    }
}
