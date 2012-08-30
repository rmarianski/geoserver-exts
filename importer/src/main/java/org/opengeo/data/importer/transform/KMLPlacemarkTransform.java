package org.opengeo.data.importer.transform;

import org.geotools.data.DataStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.styling.FeatureTypeStyle;
import org.opengeo.data.importer.ImportItem;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class KMLPlacemarkTransform extends AbstractVectorTransform implements InlineVectorTransform {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public SimpleFeatureType apply(ImportItem item, DataStore dataStore,
            SimpleFeatureType featureType) throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(featureType);
        makeStringAttribute(tb, "LookAt");
        makeStringAttribute(tb, "Region");
        makeStringAttribute(tb, "Style");
        return tb.buildFeatureType();
    }

    private void makeStringAttribute(SimpleFeatureTypeBuilder tb, String attributeName) {
        tb.remove(attributeName);
        tb.add(attributeName, String.class);
    }

    @Override
    public SimpleFeature apply(ImportItem item, DataStore dataStore, SimpleFeature oldFeature,
            SimpleFeature feature) throws Exception {
        Object lookatObj = feature.getAttribute("LookAt");
        if (lookatObj != null) {
            Coordinate lookat = (Coordinate) lookatObj;
            String serializedLookat = serializeLookAt(lookat);
            feature.setAttribute("LookAt", serializedLookat);
        }
        Object regionObj = feature.getAttribute("Region");
        if (regionObj != null) {
            Envelope envelope = (Envelope) regionObj;
            feature.setAttribute("Region", envelope.toString());
        }
        Object styleObj = feature.getAttribute("Style");
        if (styleObj != null) {
            FeatureTypeStyle style = (FeatureTypeStyle) styleObj;
            feature.setAttribute("Style", style.toString());
        }
        return feature;
    }

    private String serializeLookAt(Coordinate lookat) {
        String result = lookat.x + ", " + lookat.y;
        if (lookat.z == 0) {
            return result;
        } else {
            return result + ", " + lookat.z;
        }
    }
}
