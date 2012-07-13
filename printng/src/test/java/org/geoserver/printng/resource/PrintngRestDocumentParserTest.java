package org.geoserver.printng.resource;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.xml.serialize.XMLSerializer;
import org.junit.Test;
import org.w3c.dom.Document;

public class PrintngRestDocumentParserTest {

    @Test
    public void testParse() throws IOException {
        String input = "<div>foobar</div>";
        StringReader stringReader = new StringReader(input);
        PrintngRestDocumentParser parser = new PrintngRestDocumentParser(stringReader);
        Document document = parser.parse();

        StringWriter stringWriter = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(stringWriter, null);
        serializer.serialize(document);
        String result = stringWriter.getBuffer().toString();
        String exp = "<?xml version=\"1.0\"?>\n<div>foobar</div>";
        assertEquals("Invalid document parse", exp, result);
    }

}
