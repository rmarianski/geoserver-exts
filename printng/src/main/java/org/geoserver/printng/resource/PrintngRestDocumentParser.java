package org.geoserver.printng.resource;

import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.geoserver.rest.RestletException;
import org.restlet.data.Status;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PrintngRestDocumentParser {

    private final Reader reader;

    public PrintngRestDocumentParser(Reader reader) {
        this.reader = reader;
    }

    public Document parse() throws IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(false);
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        // resolve any referenced dtds to internal cache
        builder.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException,
                    IOException {
                String[] parts = systemId.split("/");
                String resource = "dtds/" + parts[parts.length - 1];
                return new InputSource(getClass().getResourceAsStream(resource));
            }
        });
        try {
            InputSource inputSource = new InputSource(reader);
            Document document = builder.parse(inputSource);
            return document;
        } catch (SAXException e) {
            String err = "Error parsing input xml";
            throw new RestletException(err, Status.CLIENT_ERROR_BAD_REQUEST, e);
        }
    }
}
