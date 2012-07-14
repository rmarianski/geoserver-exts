package org.geoserver.printng.reader;

import java.io.IOException;
import java.io.InputStream;

import org.geoserver.printng.iface.PrintngReader;
import org.geoserver.printng.iface.PrintngReaderFactory;
import org.geoserver.rest.RestletException;
import org.restlet.data.Request;
import org.restlet.data.Status;

public class PrintngReaderRequestBody implements PrintngReaderFactory {

    @Override
    public PrintngReader printngReader(Request request) {
        InputStream inputStream;
        try {
            inputStream = request.getEntity().getStream();
        } catch (IOException e) {
            throw new RestletException("Invalid input stream", Status.CLIENT_ERROR_BAD_REQUEST, e);
        }
        return new TemplateFromInputStream(inputStream);
    }
}
