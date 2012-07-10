package org.geoserver.printng.iface;

import org.w3c.dom.Document;

public interface PrintngWriterFactory {

    PrintngWriter printngWriter(Document document);

}
