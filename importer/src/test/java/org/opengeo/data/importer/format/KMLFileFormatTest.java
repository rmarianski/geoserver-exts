package org.opengeo.data.importer.format;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.geotools.data.FeatureReader;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

public class KMLFileFormatTest extends TestCase {

    private KMLFileFormat kmlFileFormat;

    @Override
    protected void setUp() throws Exception {
        kmlFileFormat = new KMLFileFormat();
    }

    public void testParseFeatureTypeNoPlacemarks() throws IOException {
        String kmlInput = "<kml></kml>";
        try {
            kmlFileFormat.parseFeatureType("foo", IOUtils.toInputStream(kmlInput));
        } catch (IllegalArgumentException e) {
            assertTrue(true);
            return;
        }
        fail("Expected Illegal Argument Exception for no features");
    }

    public void testParseFeatureTypeMinimal() throws Exception {
        String kmlInput = "<kml><Placemark></Placemark></kml>";
        SimpleFeatureType featureType = kmlFileFormat.parseFeatureType("foo",
                IOUtils.toInputStream(kmlInput));
        assertEquals("Unexpected number of feature type attributes", 11,
                featureType.getAttributeCount());
    }

    public void testExtendedUserData() throws Exception {
        String kmlInput = "<kml><Placemark>" + "<ExtendedData>"
                + "<Data name=\"foo\"><value>bar</value></Data>"
                + "<Data name=\"quux\"><value>morx</value></Data>" + "</ExtendedData>"
                + "</Placemark></kml>";
        SimpleFeatureType featureType = kmlFileFormat.parseFeatureType("fleem",
                IOUtils.toInputStream(kmlInput));
        assertEquals("Unexpected number of feature type attributes", 13,
                featureType.getAttributeCount());
        assertEquals("Invalid attribute descriptor", String.class, featureType.getDescriptor("foo")
                .getType().getBinding());
        assertEquals("Invalid attribute descriptor", String.class, featureType
                .getDescriptor("quux").getType().getBinding());
    }

    public void testReadFeatureWithNameAndDescription() throws Exception {
        String kmlInput = "<kml><Placemark><name>foo</name><description>bar</description></Placemark></kml>";
        SimpleFeatureType featureType = kmlFileFormat.parseFeatureType("foo",
                IOUtils.toInputStream(kmlInput));
        FeatureReader<FeatureType, Feature> reader = kmlFileFormat.read(featureType,
                IOUtils.toInputStream(kmlInput));
        assertTrue("No features found", reader.hasNext());
        SimpleFeature feature = (SimpleFeature) reader.next();
        assertNotNull("Expecting feature", feature);
        assertEquals("Invalid name attribute", "foo", feature.getAttribute("name"));
        assertEquals("Invalid description attribute", "bar", feature.getAttribute("description"));
    }

    public void testReadFeatureWithExtendedData() throws Exception {
        String kmlInput = "<kml><Placemark>" + "<ExtendedData>"
                + "<Data name=\"foo\"><value>bar</value></Data>"
                + "<Data name=\"quux\"><value>morx</value></Data>" + "</ExtendedData>"
                + "</Placemark></kml>";
        SimpleFeatureType featureType = kmlFileFormat.parseFeatureType("foo",
                IOUtils.toInputStream(kmlInput));
        FeatureReader<FeatureType, Feature> reader = kmlFileFormat.read(featureType,
                IOUtils.toInputStream(kmlInput));
        assertTrue("No features found", reader.hasNext());
        SimpleFeature feature = (SimpleFeature) reader.next();
        assertNotNull("Expecting feature", feature);
        assertEquals("Invalid ext attr foo", "bar", feature.getAttribute("foo"));
        assertEquals("Invalid ext attr quux", "morx", feature.getAttribute("quux"));
    }

}
