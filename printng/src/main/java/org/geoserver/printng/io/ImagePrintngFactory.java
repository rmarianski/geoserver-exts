package org.geoserver.printng.io;

import org.w3c.dom.Document;

public class ImagePrintngFactory implements PrintngWriterFactory {

    @Override
    public PrintngWriter printngWriter(Document document) {
        //TODO hard coded
        return new ImageWriter(document, 512, 80, "png");
    }

}
