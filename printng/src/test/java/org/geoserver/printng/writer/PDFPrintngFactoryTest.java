package org.geoserver.printng.writer;

import static org.junit.Assert.assertNotNull;

import org.geoserver.printng.iface.PrintngWriter;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;

public class PDFPrintngFactoryTest {

    @Test
    public void testPDFPrintngFactory() {
        Reference resourceRef = new Reference();
        resourceRef.setQuery("");
        Request request = new Request();
        request.setResourceRef(resourceRef);

        PDFPrintngFactory pdfPrintngFactory = new PDFPrintngFactory(request);
        PrintngWriter printngWriter = pdfPrintngFactory.printngWriter(null);
        assertNotNull(printngWriter);
    }

}
