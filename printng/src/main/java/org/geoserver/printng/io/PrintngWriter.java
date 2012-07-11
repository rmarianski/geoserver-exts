package org.geoserver.printng.io;

import java.io.IOException;
import java.io.OutputStream;

public interface PrintngWriter {

    void write(OutputStream out) throws IOException;

}
