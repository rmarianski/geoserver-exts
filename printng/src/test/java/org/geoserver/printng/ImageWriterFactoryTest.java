package org.geoserver.printng;

import static org.junit.Assert.assertNotNull;

import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.spi.ImageWriterFactory;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;

public class ImageWriterFactoryTest {

    @Test
    public void testPrintngWriter() {
        Reference resourceRef = new Reference();
        resourceRef.setQuery("");
        Request request = new Request();
        request.setResourceRef(resourceRef);
        ImageWriterFactory imagePrintngFactory = new ImageWriterFactory(request, "pdf");
        PrintngWriter printngWriter = imagePrintngFactory.printngWriter(null);
        assertNotNull(printngWriter);
    }

}
