package org.geoserver.printng.restlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.printng.api.PrintngWriter;
import org.restlet.data.MediaType;
import org.restlet.resource.StreamRepresentation;

public class PrintRepresentation extends StreamRepresentation {

    private final PrintngWriter writer;

    public PrintRepresentation(MediaType mediaType, PrintngWriter writer) {
        super(mediaType);
        this.writer = writer;
    }

    @Override
    public InputStream getStream() throws IOException {
        throw new UnsupportedOperationException("PrintRepresentation getStream");
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        writer.write(outputStream);
    }

}
