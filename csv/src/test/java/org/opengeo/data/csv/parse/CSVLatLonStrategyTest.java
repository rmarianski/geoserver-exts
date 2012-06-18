package org.opengeo.data.csv.parse;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.opengeo.data.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class CSVLatLonStrategyTest {

    private String buildInputString(String... rows) {
        StringBuilder builder = new StringBuilder();
        for (String row : rows) {
            builder.append(row);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    @Test
    public void testBuildFeatureType() {
        String input = buildInputString("lat,lon,quux,morx\n");
        CSVFileState fileState = new CSVFileState(input, "foo", WGS84);
        CSVLatLonStrategy strategy = new CSVLatLonStrategy(fileState);
        SimpleFeatureType featureType = strategy.getFeatureType();

        assertEquals("Invalid attribute count", 3, featureType.getAttributeCount());
        assertEquals("Invalid featuretype name", "foo", featureType.getName().getLocalPart());
        assertEquals("Invalid name", "foo", featureType.getTypeName());

        List<AttributeDescriptor> attrs = featureType.getAttributeDescriptors();
        assertEquals("Invalid number of attributes", 3, attrs.size());
        List<String> attrNames = new ArrayList<String>(2);
        for (AttributeDescriptor attr : attrs) {
            if (!(attr instanceof GeometryDescriptor)) {
                attrNames.add(attr.getName().getLocalPart());
            }
        }
        Collections.sort(attrNames);
        assertEquals("Invalid property descriptor", "morx", attrNames.get(0));
        assertEquals("Invalid property descriptor", "quux", attrNames.get(1));

        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        GeometryType geometryType = geometryDescriptor.getType();
        assertEquals("Invalid geometry name", "location", geometryType.getName().getLocalPart());

        CoordinateReferenceSystem crs = featureType.getCoordinateReferenceSystem();
        assertEquals("Unknown crs", WGS84, crs);
    }

    @Test
    public void testBuildFeature() throws IOException {
        String input = buildInputString("lat,lon,fleem,zoo", "3,4,car,cdr", "8,9,blub,frob");
        CSVFileState fileState = new CSVFileState(input, "bar", WGS84);
        CSVLatLonStrategy strategy = new CSVLatLonStrategy(fileState);

        CSVIterator iterator = strategy.iterator();

        assertTrue("next value not read", iterator.hasNext());
        SimpleFeature feature = iterator.next();
        Point geometry = (Point) feature.getDefaultGeometry();
        Coordinate coordinate = geometry.getCoordinate();
        assertEquals("Invalid point", 3, coordinate.y, 0.1);
        assertEquals("Invalid point", 4, coordinate.x, 0.1);
        assertEquals("Invalid feature property", "car", feature.getAttribute("fleem").toString());
        assertEquals("Invalid feature property", "cdr", feature.getAttribute("zoo").toString());

        assertTrue("next value not read", iterator.hasNext());
        feature = iterator.next();
        geometry = (Point) feature.getDefaultGeometry();
        coordinate = geometry.getCoordinate();
        assertEquals("Invalid point", 8, coordinate.y, 0.1);
        assertEquals("Invalid point", 9, coordinate.x, 0.1);
        assertEquals("Invalid feature property", "blub", feature.getAttribute("fleem").toString());
        assertEquals("Invalid feature property", "frob", feature.getAttribute("zoo").toString());
        assertFalse("extra next value", iterator.hasNext());

        try {
            iterator.next();
            fail("NoSuchElementException should have been thrown");
        } catch (NoSuchElementException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testBuildFeatureDifferentTypes() throws IOException {
        String input = buildInputString("doubleval,intval,lat,stringval,lon",
                "3.8,7,73.28,foo,-14.39", "9.12,-38,0,bar,29", "-37,0,49,baz,0");
        CSVFileState fileState = new CSVFileState(input, "typename", WGS84);
        CSVLatLonStrategy strategy = new CSVLatLonStrategy(fileState);
        CSVIterator iterator = strategy.iterator();

        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("invalid attribute count", 4, featureType.getAttributeCount());

        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        String localName = geometryDescriptor.getLocalName();
        assertEquals("Invalid geometry name", "location", localName);

        assertEquals("Invalid attribute type", "java.lang.Double",
                getBindingName(featureType, "doubleval"));
        assertEquals("Invalid attribute type", "java.lang.Integer",
                getBindingName(featureType, "intval"));
        assertEquals("Invalid attribute type", "java.lang.String",
                getBindingName(featureType, "stringval"));

        // iterate through values and verify
        Object[][] expValues = new Object[][] { new Object[] { 3.8, 7, "foo", 73.28, -14.39 },
                new Object[] { 9.12, -38, "bar", 0, 29 }, new Object[] { -37.0, 0, "baz", 49, 0 } };
        Object[] expTypes = new Object[] { Double.class, Integer.class, String.class };
        List<SimpleFeature> features = new ArrayList<SimpleFeature>(3);
        while (iterator.hasNext()) {
            features.add(iterator.next());
        }
        assertEquals("Invalid number of features", 3, features.size());

        String[] attrNames = new String[] { "doubleval", "intval", "stringval" };
        int i = 0;
        for (SimpleFeature feature : features) {
            Object[] expVals = expValues[i];
            for (int j = 0; j < 3; j++) {
                String attr = attrNames[j];
                Object value = feature.getAttribute(attr);
                Class<?> type = value.getClass();
                assertEquals("Invalid attribute type", expTypes[j], type);
                assertEquals("Invalid value", expVals[j], value);
            }
            i++;
        }
    }

    @Test
    public void testNoGeometry() throws IOException {
        String input = buildInputString("a,b", "foo,bar");
        CSVFileState fileState = new CSVFileState(input, "typename", WGS84);
        CSVLatLonStrategy strategy = new CSVLatLonStrategy(fileState);
        SimpleFeatureType featureType = strategy.getFeatureType();

        assertEquals("Invalid number of attributes", 2, featureType.getAttributeCount());
        assertNull("Expected no geometry in feature type", featureType.getGeometryDescriptor());
        assertEquals("Invalid attribute type", "java.lang.String", getBindingName(featureType, "a"));
        assertEquals("Invalid attribute type", "java.lang.String", getBindingName(featureType, "b"));

        CSVIterator iterator = strategy.iterator();
        SimpleFeature feature = iterator.next();
        assertEquals("Invalid feature attribute count", 2, feature.getAttributeCount());
        assertEquals("Invalid attribute", "foo", feature.getAttribute("a"));
        assertEquals("Invalid attribute", "bar", feature.getAttribute("b"));
    }

    @Test
    public void testOnlyLat() throws IOException {
        String input = buildInputString("lat,quux", "foo,morx");
        CSVFileState fileState = new CSVFileState(input, "typename", WGS84);
        CSVLatLonStrategy strategy = new CSVLatLonStrategy(fileState);
        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid number of attributes", 2, featureType.getAttributeCount());
        assertNull("Unexpected geometry", featureType.getGeometryDescriptor());
        assertEquals("Invalid attribute type", "java.lang.String",
                getBindingName(featureType, "lat"));
        assertEquals("Invalid attribute type", "java.lang.String",
                getBindingName(featureType, "quux"));
        CSVIterator iterator = strategy.iterator();
        SimpleFeature feature = iterator.next();
        assertEquals("Invalid lat value", "foo", feature.getAttribute("lat"));
        assertEquals("Invalid lat value", "morx", feature.getAttribute("quux"));
    }

    @Test
    public void testDataDoesNotContainAllFields() throws IOException {
        String input = buildInputString("lat,lon,foo,bar", "-72.3829,42.29,quux");
        CSVFileState fileState = new CSVFileState(input, "typename", WGS84);
        CSVLatLonStrategy strategy = new CSVLatLonStrategy(fileState);
        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid attribute count", 3, featureType.getAttributeCount());
        CSVIterator iterator = strategy.iterator();
        SimpleFeature feature = iterator.next();
        Point point = (Point) feature.getAttribute("location");
        Coordinate coordinate = point.getCoordinate();
        assertEquals("Invalid x coordinate", 42.29, coordinate.x, 0.1);
        assertEquals("Invalid y coordinate", -72.3829, coordinate.y, 0.1);
        assertEquals("Invalid attribute value", "quux", feature.getAttribute("foo"));
        assertNull("Expected null", feature.getAttribute("bar"));
    }

    @Test
    public void testDataContainsMoreFields() throws IOException {
        String input = buildInputString("lat,lon,foo", "-72.3829,42.29,quux,morx");
        CSVFileState fileState = new CSVFileState(input, "typename", WGS84);
        CSVLatLonStrategy strategy = new CSVLatLonStrategy(fileState);
        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid attribute count", 2, featureType.getAttributeCount());
        CSVIterator iterator = strategy.iterator();
        SimpleFeature feature = iterator.next();
        assertEquals("Invalid attribute count", 2, feature.getAttributeCount());
        assertNotNull("No location", feature.getAttribute("location"));
        assertEquals("Invalid attribute value", "quux", feature.getAttribute("foo"));
    }

    private String getBindingName(SimpleFeatureType featureType, String col) {
        AttributeDescriptor descriptor = featureType.getDescriptor(col);
        AttributeType attributeType = descriptor.getType();
        Class<?> binding = attributeType.getBinding();
        String bindingName = binding.getName();
        return bindingName;
    }
}
