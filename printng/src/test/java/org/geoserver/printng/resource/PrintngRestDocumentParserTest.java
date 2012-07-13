package org.geoserver.printng.resource;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.w3c.dom.Document;

public class PrintngRestDocumentParserTest {

    @Test
    public void testParse() throws IOException, TransformerException {
        String input = "<div>foobar</div>";
        StringReader stringReader = new StringReader(input);
        PrintngRestDocumentParser parser = new PrintngRestDocumentParser(stringReader);
        Document document = parser.parse();
        TransformerFactory txFactory = TransformerFactory.newInstance();
        Transformer tx = txFactory.newTransformer();
        StringWriter stringWriter = new StringWriter();
        tx.transform(new DOMSource(document), new StreamResult(stringWriter));
        String result = stringWriter.getBuffer().toString();
        String exp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><div>foobar</div>";
        assertEquals("Invalid document parse", exp, result);
    }

}
