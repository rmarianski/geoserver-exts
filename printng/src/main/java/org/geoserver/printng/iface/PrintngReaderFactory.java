package org.geoserver.printng.iface;

import org.restlet.data.Request;

public interface PrintngReaderFactory {

    PrintngReader printngReader(Request request);

}
