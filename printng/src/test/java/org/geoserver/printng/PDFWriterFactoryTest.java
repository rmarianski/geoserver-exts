package org.geoserver.printng;

import static org.junit.Assert.assertNotNull;

import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.spi.PDFWriterFactory;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;

public class PDFWriterFactoryTest {

    @Test
    public void testPDFWriterFactory() {
        Reference resourceRef = new Reference();
        resourceRef.setQuery("");
        Request request = new Request();
        request.setResourceRef(resourceRef);

        PDFWriterFactory pdfPrintngFactory = new PDFWriterFactory(request);
        PrintngWriter printngWriter = pdfPrintngFactory.printngWriter(null);
        assertNotNull(printngWriter);
    }

}
