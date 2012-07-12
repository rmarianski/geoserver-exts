package org.geoserver.printng.writer;

//import static org.easymock.classextension.EasyMock.createMock;
//import static org.easymock.classextension.EasyMock.expect;
//import static org.easymock.classextension.EasyMock.replay;
//import org.restlet.data.Form;
import static org.junit.Assert.assertNotNull;

import org.geoserver.printng.iface.PrintngWriter;
import org.junit.Test;
import org.restlet.data.Reference;
import org.restlet.data.Request;

public class PDFPrintngFactoryTest {

    @Test
    public void testPDFPrintngFactory() {
        // Form form = createMock(Form.class);
        // expect(form.getFirst("dpp")).andReturn(null);
        // expect(form.getFirst("baseURL")).andReturn(null);
        // replay(form);
        //
        // Reference resourceRef = createMock(Reference.class);
        // expect(resourceRef.getQueryAsForm()).andReturn(form);
        // replay(resourceRef);
        //
        // Request request = createMock(Request.class);
        // expect(request.getResourceRef()).andReturn(resourceRef);
        // replay(request);

        Reference resourceRef = new Reference();
        resourceRef.setQuery("");
        Request request = new Request();
        request.setResourceRef(resourceRef);

        PDFPrintngFactory pdfPrintngFactory = new PDFPrintngFactory(request);
        PrintngWriter printngWriter = pdfPrintngFactory.printngWriter(null);
        assertNotNull(printngWriter);
    }

}
