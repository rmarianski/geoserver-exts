package org.geoserver.printng.api;

import org.restlet.data.Request;

public interface PrintngReaderFactory {

    PrintngReader printngReader(Request request);

}
