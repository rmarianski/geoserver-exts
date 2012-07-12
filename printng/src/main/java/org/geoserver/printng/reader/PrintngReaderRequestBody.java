package org.geoserver.printng.reader;

import org.geoserver.printng.iface.PrintngReader;
import org.geoserver.printng.iface.PrintngReaderFactory;
import org.restlet.data.Request;

public class PrintngReaderRequestBody implements PrintngReaderFactory {

    @Override
    public PrintngReader printngReader(Request request) {
        return new TemplateFromRequestReader(request);
    }
}
