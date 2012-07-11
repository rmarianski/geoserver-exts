package org.geoserver.printng.io;

import org.restlet.data.Request;

public interface PrintngReaderFactory {

    PrintngReader printngReader(Request request);

}
