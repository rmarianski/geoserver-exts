package org.geoserver.printng.io;

import java.io.IOException;
import java.io.OutputStream;

import org.w3c.dom.Document;

public interface PrintngWriter {

    void write(Document document, OutputStream out) throws IOException;

}
