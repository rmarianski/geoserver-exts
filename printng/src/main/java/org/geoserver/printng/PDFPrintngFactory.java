package org.geoserver.printng;

import org.geoserver.printng.io.PDFWriter;
import org.geoserver.printng.io.PrintngWriter;
import org.geoserver.printng.io.PrintngWriterFactory;
import org.w3c.dom.Document;

public class PDFPrintngFactory implements PrintngWriterFactory {

    @Override
    public PrintngWriter printngWriter(Document document) {
        return new PDFWriter(document);
    }

}
