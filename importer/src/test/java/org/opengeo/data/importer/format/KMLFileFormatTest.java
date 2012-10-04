package org.opengeo.data.importer.format;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.geotools.data.FeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class KMLFileFormatTest extends TestCase {

    private KMLFileFormat kmlFileFormat;

    @Override
    protected void setUp() throws Exception {
        kmlFileFormat = new KMLFileFormat();
    }

    public void testParseFeatureTypeNoPlacemarks() throws IOException {
        String kmlInput = "<kml></kml>";
        try {
            kmlFileFormat.parseFeatureTypes("foo", IOUtils.toInputStream(kmlInput));
        } catch (IllegalArgumentException e) {
            assertTrue(true);
            return;
        }
        fail("Expected Illegal Argument Exception for no features");
    }

    public void testParseFeatureTypeMinimal() throws Exception {
        String kmlInput = "<kml><Placemark></Placemark></kml>";
        List<SimpleFeatureType> featureTypes = kmlFileFormat.parseFeatureTypes("foo",
                IOUtils.toInputStream(kmlInput));
        assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        assertEquals("Unexpected number of feature type attributes", 10,
                featureType.getAttributeCount());
    }

    public void testExtendedUserData() throws Exception {
        String kmlInput = "<kml><Placemark>" + "<ExtendedData>"
                + "<Data name=\"foo\"><value>bar</value></Data>"
                + "<Data name=\"quux\"><value>morx</value></Data>" + "</ExtendedData>"
                + "</Placemark></kml>";
        List<SimpleFeatureType> featureTypes = kmlFileFormat.parseFeatureTypes("fleem",
                IOUtils.toInputStream(kmlInput));
        assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        assertEquals("Unexpected number of feature type attributes", 12,
                featureType.getAttributeCount());
        assertEquals("Invalid attribute descriptor", String.class, featureType.getDescriptor("foo")
                .getType().getBinding());
        assertEquals("Invalid attribute descriptor", String.class, featureType
                .getDescriptor("quux").getType().getBinding());
    }

    public void testReadFeatureWithNameAndDescription() throws Exception {
        String kmlInput = "<kml><Placemark><name>foo</name><description>bar</description></Placemark></kml>";
        SimpleFeatureType featureType = kmlFileFormat.parseFeatureTypes("foo",
                IOUtils.toInputStream(kmlInput)).get(0);
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = kmlFileFormat.read(featureType,
                IOUtils.toInputStream(kmlInput));
        assertTrue("No features found", reader.hasNext());
        SimpleFeature feature = reader.next();
        assertNotNull("Expecting feature", feature);
        assertEquals("Invalid name attribute", "foo", feature.getAttribute("name"));
        assertEquals("Invalid description attribute", "bar", feature.getAttribute("description"));
    }

    public void testReadFeatureWithUntypedExtendedData() throws Exception {
        String kmlInput = "<kml><Placemark>" + "<ExtendedData>"
                + "<Data name=\"foo\"><value>bar</value></Data>"
                + "<Data name=\"quux\"><value>morx</value></Data>" + "</ExtendedData>"
                + "</Placemark></kml>";
        SimpleFeatureType featureType = kmlFileFormat.parseFeatureTypes("foo",
                IOUtils.toInputStream(kmlInput)).get(0);
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = kmlFileFormat.read(featureType,
                IOUtils.toInputStream(kmlInput));
        assertTrue("No features found", reader.hasNext());
        SimpleFeature feature = (SimpleFeature) reader.next();
        assertNotNull("Expecting feature", feature);
        assertEquals("Invalid ext attr foo", "bar", feature.getAttribute("foo"));
        assertEquals("Invalid ext attr quux", "morx", feature.getAttribute("quux"));
    }

    public void testReadFeatureWithTypedExtendedData() throws Exception {
        String kmlInput = "<kml>" + "<Schema name=\"myschema\">"
                + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>" + "</Schema>"
                + "<Placemark>" + "<ExtendedData>" + "<SchemaData schemaUrl=\"#myschema\">"
                + "<SimpleData name=\"foo\">42</SimpleData>" + "</SchemaData>" + "</ExtendedData>"
                + "</Placemark></kml>";
        SimpleFeatureType featureType = kmlFileFormat.parseFeatureTypes("foo",
                IOUtils.toInputStream(kmlInput)).get(0);
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = kmlFileFormat.read(featureType,
                IOUtils.toInputStream(kmlInput));
        assertTrue("No features found", reader.hasNext());
        SimpleFeature feature = reader.next();
        assertNotNull("Expecting feature", feature);
        assertEquals("Invalid ext attr foo", 42, feature.getAttribute("foo"));
    }

    public void testMultipleSchemas() throws Exception {
        String kmlInput = "<kml>" + "<Schema name=\"schema1\">"
                + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>" + "</Schema>"
                + "<Schema name=\"schema2\">"
                + "<SimpleField type=\"float\" name=\"bar\"></SimpleField>" + "</Schema>"
                + "<Placemark>" + "<ExtendedData>" + "<SchemaData schemaUrl=\"#schema1\">"
                + "<SimpleData name=\"foo\">42</SimpleData>" + "</SchemaData>"
                + "<SchemaData schemaUrl=\"#schema2\">"
                + "<SimpleData name=\"bar\">4.2</SimpleData>" + "</SchemaData>" + "</ExtendedData>"
                + "</Placemark></kml>";
        List<SimpleFeatureType> featureTypes = kmlFileFormat.parseFeatureTypes(kmlInput,
                IOUtils.toInputStream(kmlInput));
        assertEquals("Unexpected number of feature types", 2, featureTypes.size());
        SimpleFeatureType ft1 = featureTypes.get(0);
        SimpleFeatureType ft2 = featureTypes.get(1);

        FeatureReader<SimpleFeatureType, SimpleFeature> reader1 = kmlFileFormat.read(ft1,
                IOUtils.toInputStream(kmlInput));
        SimpleFeature feature1 = reader1.next();
        assertNotNull("Expecting feature", feature1);
        assertEquals("Invalid ext attr foo", 42, feature1.getAttribute("foo"));
        assertNull("Invalid attribute for first schema", feature1.getAttribute("bar"));

        FeatureReader<SimpleFeatureType, SimpleFeature> reader2 = kmlFileFormat.read(ft2,
                IOUtils.toInputStream(kmlInput));
        SimpleFeature feature2 = reader2.next();
        assertNotNull("Expecting feature", feature2);
        assertNull("Invalid attribute for second schema", feature2.getAttribute("foo"));
        assertEquals("Invalid ext attr bar", 4.2f, (Float) feature2.getAttribute("bar"), 0.01);
    }

    public void testTypedAndUntyped() throws Exception {
        String kmlInput = "<kml>" + "<Schema name=\"myschema\">"
                + "<SimpleField type=\"int\" name=\"foo\"></SimpleField>" + "</Schema>"
                + "<Placemark>" + "<ExtendedData>" + "<SchemaData schemaUrl=\"#myschema\">"
                + "<SimpleData name=\"foo\">42</SimpleData>" + "</SchemaData>"
                + "<Data name=\"fleem\"><value>bar</value></Data>"
                + "<Data name=\"quux\"><value>morx</value></Data>" + "</ExtendedData>"
                + "</Placemark></kml>";
        List<SimpleFeatureType> featureTypes = kmlFileFormat.parseFeatureTypes(kmlInput,
                IOUtils.toInputStream(kmlInput));
        assertEquals("Unexpected number of feature types", 1, featureTypes.size());
        SimpleFeatureType featureType = featureTypes.get(0);
        FeatureReader<SimpleFeatureType, SimpleFeature> reader = kmlFileFormat.read(featureType,
                IOUtils.toInputStream(kmlInput));
        SimpleFeature feature = reader.next();
        assertNotNull("Expecting feature", feature);
        assertEquals("Invalid ext attr foo", 42, feature.getAttribute("foo"));
        assertEquals("bar", feature.getAttribute("fleem"));
        assertEquals("morx", feature.getAttribute("quux"));
    }
}
