package org.geoserver.printng.io;

import org.w3c.dom.Document;

public interface PrintngWriterFactory {

    PrintngWriter printngWriter(Document document);

}
