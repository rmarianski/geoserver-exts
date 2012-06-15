package org.opengeo.data.csv.parse;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.opengeo.data.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
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
        CSVFileState fileState = new CSVFileState(input, "foo", WGS84, null);
        CSVLatLonStrategy strategy = new CSVLatLonStrategy(fileState);
        SimpleFeatureType featureType = strategy.getFeatureType();

        assertEquals("Invalid attribute count", 3, featureType.getAttributeCount());
        assertEquals("Invalid featuretype name", "foo", featureType.getName().getLocalPart());
        assertEquals("Invalid name", "foo", featureType.getTypeName());

        List<AttributeDescriptor> attrs = featureType.getAttributeDescriptors();
        assertEquals("Invalid number of attributes", 3, attrs.size());
        AttributeDescriptor ad1 = attrs.get(1);
        assertEquals("Invalid property descriptor", "quux", ad1.getName().getLocalPart());
        AttributeDescriptor ad2 = attrs.get(2);
        assertEquals("Invalid property descriptor", "morx", ad2.getName().getLocalPart());

        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        GeometryType geometryType = geometryDescriptor.getType();
        assertEquals("Invalid geometry name", "location", geometryType.getName().getLocalPart());

        CoordinateReferenceSystem crs = featureType.getCoordinateReferenceSystem();
        assertEquals("Unknown crs", WGS84, crs);
    }

    @Test
    public void testBuildFeature() throws IOException {
        String input = buildInputString("lat,lon,fleem,zoo", "3,4,car,cdr", "8,9,blub,frob");
        CSVFileState fileState = new CSVFileState(input, "bar", WGS84, null);
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
}
