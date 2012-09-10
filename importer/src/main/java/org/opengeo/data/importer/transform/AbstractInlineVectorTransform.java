package org.opengeo.data.importer.transform;

import org.geotools.data.DataStore;
import org.opengeo.data.importer.ImportItem;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Convenience base class to make creating inline vector transforms easier
 * 
 */
public abstract class AbstractInlineVectorTransform extends AbstractVectorTransform implements
        InlineVectorTransform {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public SimpleFeatureType apply(ImportItem item, DataStore dataStore,
            SimpleFeatureType featureType) throws Exception {
        return featureType;
    }

    @Override
    public SimpleFeature apply(ImportItem item, DataStore dataStore, SimpleFeature oldFeature,
            SimpleFeature feature) throws Exception {
        return feature;
    }

}
