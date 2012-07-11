package org.geoserver.printng.io;

import org.restlet.data.Request;

public class PrintngReaderRequestBody implements PrintngReaderFactory {

    @Override
    public PrintngReader printngReader(Request request) {
        return new TemplateFromRequestReader(request);
    }
}
