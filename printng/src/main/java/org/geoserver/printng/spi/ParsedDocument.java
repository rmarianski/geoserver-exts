package org.geoserver.printng.spi;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.ccil.cowan.tagsoup.Parser;
import org.geoserver.rest.RestletException;
import org.restlet.data.Status;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ParsedDocument {

    private final Document dom;
    private final String baseURL;

    private ParsedDocument(Document dom, String baseURL) {
        this.dom = dom;
        this.baseURL = baseURL;
    }

    public static ParsedDocument parse(String src) throws IOException {
        return parse(new StringReader(src));
    }

    public static ParsedDocument parse(Reader src) throws IOException {
        return parse(src, true);
    }

    public static ParsedDocument parse(Reader src, boolean useTagSoup) throws IOException {
        return new ParsedDocument(parseDocument(src, useTagSoup), null);
    }

    public static ParsedDocument parse(File src, boolean useTagSoup) throws IOException {
        return new ParsedDocument(parseDocument(new FileReader(src), useTagSoup), 
            src.toURI().toURL().toString());
    }

    public String getBaseURL() {
        return baseURL;
    }

    public Document getDocument() {
        return dom;
    }

    public static Document parseDocument(Reader reader, boolean useTagSoup) throws IOException {
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
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (Exception e) {
            String err = "Error creating xml transformer";
            throw new RestletException(err, Status.SERVER_ERROR_INTERNAL, e);
        }
        try {
            Parser parser = new Parser();
            InputSource inputSource = new InputSource(reader);
            DOMResult domResult = new DOMResult();
            if (useTagSoup) {
                transformer.transform(new SAXSource(parser, inputSource), domResult);
            } else {
                transformer.transform(new SAXSource(inputSource), domResult);
            }
            Document document = (Document) domResult.getNode();
            return document;
        } catch (TransformerException e) {
            String err = "Error parsing input xml";
            throw new RestletException(err, Status.CLIENT_ERROR_BAD_REQUEST, e);
        }
    }
}
