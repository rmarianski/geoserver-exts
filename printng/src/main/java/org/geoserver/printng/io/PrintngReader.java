package org.geoserver.printng.io;

import java.io.IOException;
import java.io.Reader;

public interface PrintngReader {

    Reader reader() throws IOException;

}
