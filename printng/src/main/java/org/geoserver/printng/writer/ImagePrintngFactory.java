package org.geoserver.printng.writer;

import org.geoserver.printng.iface.PrintngWriter;
import org.geoserver.printng.iface.PrintngWriterFactory;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.w3c.dom.Document;

public class ImagePrintngFactory implements PrintngWriterFactory {

    private final String format;

    private final int width;

    private final int height;

    private final Integer dpp;

    public ImagePrintngFactory(Request request, String format) {
        this.format = format;
        Form form = request.getResourceRef().getQueryAsForm();
        Parameter width = form.getFirst("width");
        Parameter height = form.getFirst("height");
        Parameter dpp = form.getFirst("dpp");
        this.width = parseInt(width, 512);
        this.height = parseInt(height, 256);
        this.dpp = parseInt(dpp, 0);
    }

    private int parseInt(Parameter parameter, Integer defaultValue) {
        if (parameter == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(parameter.getValue());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public PrintngWriter printngWriter(Document document) {
        return new ImageWriter(document, this.width, this.height, format, dpp);
    }
}
