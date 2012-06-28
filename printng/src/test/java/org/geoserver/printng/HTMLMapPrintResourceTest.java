/*
 */
package org.geoserver.printng;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HTMLMapPrintResourceTest extends PrintTestSupport {

    public void testRenderContentNegotation() throws Exception {
        MockHttpServletResponse response = postAsServletResponse("/rest/printng/render.png", "<div>FOOBAR</div>");
        checkImage(response,"image/png");
        
        response = postAsServletResponse("/rest/printng/render.jpg", "<div>FOOBAR</div>");
        checkImage(response,"image/jpeg");
        
        response = postAsServletResponse("/rest/printng/render.pdf", "<div>FOOBAR</div>");
        checkPDF(response);
    }
    
}
