package org.opengeo.data.importer.transform;

import java.util.List;

import org.geotools.data.DataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.styling.FeatureTypeStyle;
import org.opengeo.data.importer.FeatureDataConverter;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.format.KMLFileFormat;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Geometry;

public class KMLPlacemarkTransform extends AbstractVectorTransform implements InlineVectorTransform {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public SimpleFeatureType convertFeatureType(SimpleFeatureType oldFeatureType) {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.add("Geometry", Geometry.class);
        ftb.setDefaultGeometry("Geometry");
        List<AttributeDescriptor> attributeDescriptors = oldFeatureType.getAttributeDescriptors();
        for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
            String localName = attributeDescriptor.getLocalName();
            if (!"Geometry".equals(localName)) {
                ftb.add(attributeDescriptor);
            }
        }
        ftb.setName(oldFeatureType.getName());
        ftb.setDescription(oldFeatureType.getDescription());
        ftb.setCRS(KMLFileFormat.KML_CRS);
        ftb.setSRS(KMLFileFormat.KML_SRS);
        makeStringAttribute(ftb, "Style");
        SimpleFeatureType ft = ftb.buildFeatureType();
        return ft;
    }

    public SimpleFeature convertFeature(SimpleFeature old, SimpleFeatureType targetFeatureType) {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetFeatureType);
        SimpleFeature newFeature = fb.buildFeature(old.getID());
        FeatureDataConverter.DEFAULT.convert(old, newFeature);
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
}
