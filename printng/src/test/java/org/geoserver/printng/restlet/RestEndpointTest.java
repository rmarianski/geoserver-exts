package org.geoserver.printng.restlet;

import static testsupport.PrintTestSupport.assertPDF;
import static testsupport.PrintTestSupport.assertPNG;
import static testsupport.PrintTestSupport.assertTemplateExists;
import static testsupport.PrintTestSupport.form;
import junit.framework.Test;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.test.GeoServerTestSupport;
import org.restlet.data.MediaType;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class RestEndpointTest extends GeoServerTestSupport {

    String path;
    String body;
    String contentType;
    int width;
    int height;
    int status = 200;
    String[] formData;
    String requestContentType = "text/xml";

    // make sure we don't recreate the data dir constantly
    public static Test suite() {
        return new OneTimeTestSetup(new RestEndpointTest());
    }

    public void testValidPngRender() throws Exception {
        path = "/rest/printng/render.png";
        body = "<div>FOOBAR</div>";
        contentType = "image/png";
        runPostTest();
    }
    
    public void testValidPngRenderSize() throws Exception {
        // specify native size in document
        path = "/rest/printng/render.png";
        body = "<div style='width:25px;height:25px;'>FOOBAR</div>";
        contentType = "image/png";
        width = 25;
        height = 25;
        runPostTest();
        
        // now scale the output
        width = 50;
        height = 50;
        path = "/rest/printng/render.png?width=50&height=50";
        runPostTest();

    }

    public void testValidPdfRender() throws Exception {
        path = "/rest/printng/render.pdf";
        body = "<div>FOOBAR</div>";
        contentType = "application/pdf";
        runPostTest();
    }

    public void testValidHtmlRender() throws Exception {
        path = "/rest/printng/render.html";
        body = "<div>FOOBAR</div>";
        contentType = "text/html";
        runPostTest();
    }

    public void testPOSTTemplate() throws Exception {
        // create a template
        path = "/rest/printng/freemarker/foobar";
        body = "<div>${msg}</div>";
        contentType = "text/plain";
        status = 201;
        runPostTest();
        assertTemplateExists("foobar.ftl");

        // render png w/ get params
        path = "/rest/printng/freemarker/foobar.png?msg=BAR";
        body = "";
        contentType = "image/png";
        status = 200;
        runPostTest();

        // as html
        path = "/rest/printng/freemarker/foobar.html?msg=BAR";
        contentType = "text/html";
        MockHttpServletResponse resp = runPostTest();
        assertTrue(resp.getOutputStreamContent().indexOf("BAR") > 0);

        // and with post body
        path = "/rest/printng/freemarker/foobar.html";
        formData = new String[]{"msg", "BAR"};
        resp = runPostTest();
        assertTrue(resp.getOutputStreamContent().indexOf("BAR") > 0);
    }

    private MockHttpServletResponse assertResponse(MockHttpServletResponse resp) throws Exception {
        assertEquals(status, resp.getStatusCode());
        String type = resp.getContentType().split(";")[0];
        assertEquals(contentType, type);
        if (contentType.equals("image/png")) {
            if (width == 0) {
                PrintSpec defaults = new PrintSpec(null).useDefaultRenderDimension();
                width = defaults.getOutputWidth();
                height = defaults.getOutputHeight();
            }
            assertPNG(getBinaryInputStream(resp), width, height);
        } else if (contentType.equals("application/pdf")) {
            assertPDF(getBinaryInputStream(resp));
        }
        return resp;
    }

    private MockHttpServletResponse runPostTest() throws Exception {
        if (formData != null) {
            body = form(formData).getWebRepresentation().getText();
            requestContentType = MediaType.APPLICATION_WWW_FORM.toString();
        }
        return assertResponse(postAsServletResponse(path, body, requestContentType));
    }
}
