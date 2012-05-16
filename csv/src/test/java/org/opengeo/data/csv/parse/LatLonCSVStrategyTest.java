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

import com.vividsolutions.jts.geom.Coordinate;

public class LatLonCSVStrategyTest {

    @Test
    public void testBuildFeatureType() {
        LatLonCSVStrategy strategy = new LatLonCSVStrategy("foo", WGS84, new String[] { "lat",
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
    }

    @Test
    public void testBuildFeature() {
        LatLonCSVStrategy strategy = new LatLonCSVStrategy("bar", WGS84, new String[] { "lat",
                "lon", "fleem", "zoo" });

        SimpleFeature feature = strategy.buildFeature(new String[] { "3", "4", "car", "cdr" });
        Coordinate geometry = (Coordinate) feature.getDefaultGeometry();
        assertEquals("Invalid point", 3, geometry.y, 0.1);
        assertEquals("Invalid point", 4, geometry.x, 0.1);

        assertEquals("Invalid feature property", "car", feature.getAttribute("fleem").toString());
        assertEquals("Invalid feature property", "cdr", feature.getAttribute("zoo").toString());
    }
}
