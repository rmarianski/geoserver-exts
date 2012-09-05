package org.opengeo.data.importer.transform;

import org.geotools.data.DataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.styling.FeatureTypeStyle;
import org.opengeo.data.importer.FeatureDataConverter;
import org.opengeo.data.importer.ImportItem;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class KMLPlacemarkTransform extends AbstractVectorTransform implements InlineVectorTransform {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public SimpleFeatureType convertFeatureType(SimpleFeatureType oldFeatureType) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(oldFeatureType);
        makeStringAttribute(tb, "LookAt");
        makeStringAttribute(tb, "Region");
        makeStringAttribute(tb, "Style");
        return tb.buildFeatureType();
    }

    public SimpleFeature convertFeature(SimpleFeature old, SimpleFeatureType targetFeatureType) {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetFeatureType);
        SimpleFeature newFeature = fb.buildFeature(old.getID());
        FeatureDataConverter.DEFAULT.convert(old, newFeature);
        Object lookatObj = old.getAttribute("LookAt");
        if (lookatObj != null) {
            Coordinate lookat = (Coordinate) lookatObj;
            String serializedLookat = serializeLookAt(lookat);
            newFeature.setAttribute("LookAt", serializedLookat);
        }
        Object regionObj = old.getAttribute("Region");
        if (regionObj != null) {
            Envelope envelope = (Envelope) regionObj;
            newFeature.setAttribute("Region", envelope.toString());
        }
        Object styleObj = old.getAttribute("Style");
        if (styleObj != null) {
            FeatureTypeStyle style = (FeatureTypeStyle) styleObj;
            newFeature.setAttribute("Style", style.toString());
        }
        return newFeature;
    }

    @Override
    public SimpleFeatureType apply(ImportItem item, DataStore dataStore,
            SimpleFeatureType featureType) throws Exception {
        return convertFeatureType(featureType);
    }

    private void makeStringAttribute(SimpleFeatureTypeBuilder tb, String attributeName) {
        tb.remove(attributeName);
        tb.add(attributeName, String.class);
    }

    @Override
    public SimpleFeature apply(ImportItem item, DataStore dataStore, SimpleFeature oldFeature,
            SimpleFeature feature) throws Exception {
        SimpleFeatureType targetFeatureType = feature.getFeatureType();
        SimpleFeature newFeature = convertFeature(oldFeature, targetFeatureType);
        feature.setAttributes(newFeature.getAttributes());
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
