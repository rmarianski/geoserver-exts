package org.opengeo.data.csv.parse;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class CSVLatLonStrategyTest {

    @Test
    public void testBuildFeatureType() {
        CSVLatLonStrategy strategy = new CSVLatLonStrategy("foo", WGS84, new String[] { "lat",
                "lon", "quux", "morx" });
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
    public void testBuildFeature() {
        CSVLatLonStrategy strategy = new CSVLatLonStrategy("bar", WGS84, new String[] { "lat",
                "lon", "fleem", "zoo" });

        SimpleFeature feature = strategy.buildFeature(new String[] { "3", "4", "car", "cdr" });
        Point geometry = (Point) feature.getDefaultGeometry();
        Coordinate coordinate = geometry.getCoordinate();
        assertEquals("Invalid point", 3, coordinate.y, 0.1);
        assertEquals("Invalid point", 4, coordinate.x, 0.1);

        assertEquals("Invalid feature property", "car", feature.getAttribute("fleem").toString());
        assertEquals("Invalid feature property", "cdr", feature.getAttribute("zoo").toString());
    }
}
