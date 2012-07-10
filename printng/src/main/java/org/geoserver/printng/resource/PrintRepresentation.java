package org.geoserver.printng.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.printng.io.PrintngWriter;
import org.restlet.data.MediaType;
import org.restlet.resource.StreamRepresentation;
import org.w3c.dom.Document;

public class PrintRepresentation extends StreamRepresentation {

    private final Document document;

    private final PrintngWriter writer;

    public PrintRepresentation(MediaType mediaType, Document document, PrintngWriter writer) {
        super(mediaType);
        this.document = document;
        this.writer = writer;
    }

    @Override
    public InputStream getStream() throws IOException {
        throw new UnsupportedOperationException("PrintRepresentation getStream");
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        writer.write(document, outputStream);
    }

}
