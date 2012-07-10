package org.geoserver.printng;

import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;
import org.restlet.data.Status;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class RestEndpointTest extends GeoServerTestSupport {

    @Test
    public void testValidPngRender() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("/rest/printng/render.png",
                "<div>FOOBAR</div>");
        assertEquals("Invalid response", Status.SUCCESS_OK.getCode(), response.getStatusCode());
    }

}
