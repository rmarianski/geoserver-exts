package org.geoserver.printng.api;

import org.w3c.dom.Document;

public interface PrintngWriterFactory {

    PrintngWriter printngWriter(Document document);

}
