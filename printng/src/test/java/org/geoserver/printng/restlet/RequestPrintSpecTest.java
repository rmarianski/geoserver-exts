package org.geoserver.printng.restlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.geoserver.printng.api.PrintSpec;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;

public class RequestPrintSpecTest {

    @Test
    public void testEmptyRequest() {
        Request request = new Request();
        request.setResourceRef(new Reference());
        PrintSpec printSpec = new RequestPrintSpec(request);
        assertNull(printSpec.getBaseURL());
        assertNull(printSpec.getDotsPerPixel());
        assertNull(printSpec.getWidth());
        assertNull(printSpec.getHeight());
    }

    @Test
    public void testRequestValuesSet() {
        Request request = new Request();
        Reference reference = new Reference();
        reference.setPath("/unused");
        reference.setQuery("baseURL=foo&dpp=5&width=500&height=80");
        request.setResourceRef(reference);
        PrintSpec printSpec = new RequestPrintSpec(request);
        assertEquals("foo", printSpec.getBaseURL());
        assertEquals(new Integer(5), printSpec.getDotsPerPixel());
        assertEquals(new Integer(500), printSpec.getWidth());
        assertEquals(new Integer(80), printSpec.getHeight());
    }

}
