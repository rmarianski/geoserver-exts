package org.geoserver.printng.writer;

import static org.junit.Assert.assertNotNull;

import org.geoserver.printng.iface.PrintngWriter;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;

public class ImagePrintngFactoryTest {

    @Test
    public void testPrintngWriter() {
        Reference resourceRef = new Reference();
        resourceRef.setQuery("");
        Request request = new Request();
        request.setResourceRef(resourceRef);
        ImagePrintngFactory imagePrintngFactory = new ImagePrintngFactory(request, "pdf");
        PrintngWriter printngWriter = imagePrintngFactory.printngWriter(null);
        assertNotNull(printngWriter);
    }

}
