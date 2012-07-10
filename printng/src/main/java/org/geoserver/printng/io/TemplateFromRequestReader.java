package org.geoserver.printng.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.restlet.data.Request;

public class TemplateFromRequestReader implements PrintngReader {

    private Request request;

    public TemplateFromRequestReader(Request request) {
        this.request = request;
    }

    @Override
    public Reader reader() throws IOException {
        InputStream stream = request.getEntity().getStream();
        return new InputStreamReader(stream);
    }

}
