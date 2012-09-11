package org.geoserver.printng.api;

import java.io.IOException;
import org.restlet.data.Request;

public interface ReaderSource {

    PrintngReader printngReader(Request request) throws IOException;

}
