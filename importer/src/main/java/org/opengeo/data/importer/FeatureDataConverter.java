package org.opengeo.data.importer;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

/**
 * Converts feature between two feature data sources.
 * <p>
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class FeatureDataConverter {
    
    private FeatureDataConverter() {
    }

    public SimpleFeatureType convertType(SimpleFeatureType featureType, String typeName) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(typeName);
        typeBuilder.addAll(featureType.getAttributeDescriptors());
        return typeBuilder.buildFeatureType();
    }

    public void convert(SimpleFeature from, SimpleFeature to) {
        to.setAttributes(from.getAttributes());
    }

    public static FeatureDataConverter DEFAULT = new FeatureDataConverter();

    public static FeatureDataConverter TO_SHAPEFILE = new FeatureDataConverter() {
        public SimpleFeatureType convertType(SimpleFeatureType featureType, String typeName) {
            //for shapefile we always ensure the geometry is the first type, and we have to deal
            // with the max field name length of 10
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            GeometryDescriptor g = featureType.getGeometryDescriptor();
            if (g != null) {
                typeBuilder.add(attName(g.getLocalName()), g.getType().getBinding());
            }
            for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
                if (att.equals(g)) {
                    continue;
                }
                typeBuilder.add(attName(att.getLocalName()), att.getType().getBinding());
            }
            return typeBuilder.buildFeatureType();
        }

        public void convert(SimpleFeature from, SimpleFeature to) {
            for (AttributeDescriptor att : from.getType().getAttributeDescriptors()) {
                to.setAttribute(attName(att.getLocalName()), from.getAttribute(att.getLocalName()));
            }
        }

        String attName(String name) {
            return name.length() > 10 ? name.substring(0,10) : name;
        }
    };
}
