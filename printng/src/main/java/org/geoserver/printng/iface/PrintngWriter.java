package org.geoserver.printng.iface;

import java.io.IOException;
import java.io.OutputStream;

public interface PrintngWriter {

    void write(OutputStream out) throws IOException;

}
