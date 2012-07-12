package org.geoserver.printng.writer;

import org.geoserver.printng.iface.PrintngWriter;
import org.geoserver.printng.iface.PrintngWriterFactory;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.w3c.dom.Document;

public class PDFPrintngFactory implements PrintngWriterFactory {

    private final Integer dpp;
    private final String baseURL;

    public PDFPrintngFactory(Request request) {
        Form form = request.getResourceRef().getQueryAsForm();
        Integer dppValue = null;
        Parameter dppParam = form.getFirst("dpp");
        Parameter baseURLParam = form.getFirst("baseURL");
        if (dppParam != null) {
            try {
                dppValue = Integer.parseInt(dppParam.getValue());
            } catch (NumberFormatException e) {
                dppValue = null;
            }
        }
        this.dpp = dppValue;
        this.baseURL = baseURLParam == null ? null : baseURLParam.getValue();
    }

    @Override
    public PrintngWriter printngWriter(Document document) {
        return new PDFWriter(document, dpp, baseURL);
    }

}
