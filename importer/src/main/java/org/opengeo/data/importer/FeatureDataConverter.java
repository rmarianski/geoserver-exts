package org.opengeo.data.importer;

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.data.FeatureReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Converts feature between two feature data sources.
 * <p>
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class FeatureDataConverter {

    static Logger LOGGER = Logging.getLogger(Importer.class);

    private FeatureDataConverter() {
    }

    public SimpleFeatureType convertType(SimpleFeatureType featureType, VectorFormat format, 
        ImportData data, ImportItem item) {

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(featureType.getTypeName());
        typeBuilder.addAll(featureType.getAttributeDescriptors());
        return typeBuilder.buildFeatureType();
    }

    public void convert(SimpleFeature from, SimpleFeature to) {
        to.setAttributes(from.getAttributes());
    }

    public static FeatureDataConverter DEFAULT = new FeatureDataConverter();

    public static FeatureDataConverter TO_SHAPEFILE = new FeatureDataConverter() {
        @Override
        public SimpleFeatureType convertType(SimpleFeatureType featureType, VectorFormat format, 
            ImportData data, ImportItem item) {

            //for shapefile we always ensure the geometry is the first type, and we have to deal
            // with the max field name length of 10
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.setName(featureType.getTypeName());

            GeometryDescriptor gd = featureType.getGeometryDescriptor();
            if (gd != null) {
                Class binding = gd.getType().getBinding();
                if (Geometry.class.equals(binding)) {
                    try {
                        FeatureReader r = (FeatureReader) format.read(data, item);
                        try {
                            if (r.hasNext()) {
                                SimpleFeature f = (SimpleFeature) r.next();
                                if (f.getDefaultGeometry() != null) {
                                    binding = f.getDefaultGeometry().getClass();
                                }
                            }
                        }
                        finally {
                            r.close();
                        }
                    } catch (IOException e) {
                        LOGGER.warning("Unable to determine concrete geometry type");
                    }
                }
                typeBuilder.add(attName(gd.getLocalName()), binding);
            }
            for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
                if (att.equals(gd)) {
                    continue;
                }
                typeBuilder.add(attName(att.getLocalName()), att.getType().getBinding());
            }
            return typeBuilder.buildFeatureType();
        }

        @Override
        public void convert(SimpleFeature from, SimpleFeature to) {
            for (AttributeDescriptor att : from.getType().getAttributeDescriptors()) {
                Object obj = from.getAttribute(att.getLocalName());
                if (att instanceof GeometryDescriptor) {
                    to.setDefaultGeometry(obj);
                }
                else {
                    to.setAttribute(attName(att.getLocalName()), obj);
                }
            }
        }

        String attName(String name) {
            return name.length() > 10 ? name.substring(0,10) : name;
        }
    };
}
