package org.opengeo.data.importer.transform;

import junit.framework.TestCase;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.styling.FeatureTypeStyle;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class KMLPlacemarkTransformTest extends TestCase {

    private KMLPlacemarkTransform kmlPlacemarkTransform;

    private SimpleFeatureType origType;

    private SimpleFeatureType transformedType;

    @Override
    protected void setUp() throws Exception {
        kmlPlacemarkTransform = new KMLPlacemarkTransform();

        SimpleFeatureTypeBuilder origBuilder = new SimpleFeatureTypeBuilder();
        origBuilder.setName("origtype");
        origBuilder.add("name", String.class);
        origBuilder.add("description", String.class);
        origBuilder.add("LookAt", Coordinate.class);
        origBuilder.add("Region", Envelope.class);
        origBuilder.add("Style", FeatureTypeStyle.class);
        origBuilder.add("Geometry", Geometry.class);
        origBuilder.setDefaultGeometry("Geometry");
        origType = origBuilder.buildFeatureType();

        SimpleFeatureTypeBuilder transformedBuilder = new SimpleFeatureTypeBuilder();
        transformedBuilder.setName("transformedtype");
        transformedBuilder.add("name", String.class);
        transformedBuilder.add("description", String.class);
        transformedBuilder.add("LookAt", String.class);
        transformedBuilder.add("Region", String.class);
        transformedBuilder.add("Style", String.class);
        transformedBuilder.add("Geometry", Geometry.class);
        transformedBuilder.setDefaultGeometry("Geometry");
        transformedType = transformedBuilder.buildFeatureType();
    }

    public void testFeatureType() throws Exception {
        SimpleFeatureType result = kmlPlacemarkTransform.convertFeatureType(origType);
        assertBinding(result, "LookAt", String.class);
        assertBinding(result, "Region", String.class);
        assertBinding(result, "Style", String.class);
    }

    private void assertBinding(SimpleFeatureType ft, String attr, Class<?> expectedBinding) {
        AttributeDescriptor descriptor = ft.getDescriptor(attr);
        Class<?> binding = descriptor.getType().getBinding();
        assertEquals(expectedBinding, binding);
    }

    public void testGeometry() throws Exception {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(origType);
        GeometryFactory gf = new GeometryFactory();
        fb.set("Geometry", gf.createPoint(new Coordinate(3d, 4d)));
        SimpleFeature feature = fb.buildFeature("testgeometry");
        assertEquals("Unexpected Geometry class", Point.class, feature.getAttribute("Geometry")
                .getClass());
        assertEquals("Unexpected default geometry", Point.class, feature.getDefaultGeometry()
                .getClass());
        SimpleFeature result = kmlPlacemarkTransform.convertFeature(feature, transformedType);
        assertEquals("Invalid Geometry class", Point.class, result.getAttribute("Geometry")
                .getClass());
        assertEquals("Unexpected default geometry", Point.class, feature.getDefaultGeometry()
                .getClass());
    }

    public void testLookAtProperty() throws Exception {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(origType);
        fb.set("LookAt", new Coordinate(3d, 4d));
        SimpleFeature feature = fb.buildFeature("testlookat");
        assertEquals("Unexpected LookAt attribute class", Coordinate.class,
                feature.getAttribute("LookAt").getClass());
        SimpleFeature result = kmlPlacemarkTransform.convertFeature(feature, transformedType);
        assertEquals("Invalid LookAt attribute class", String.class, result.getAttribute("LookAt")
                .getClass());
    }

}
