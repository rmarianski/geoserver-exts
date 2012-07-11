package org.geoserver.printng.io;

import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.w3c.dom.Document;

public class ImagePrintngFactory implements PrintngWriterFactory {

    private final String format;

    private int width;

    private int height;

    public ImagePrintngFactory(Request request, String format) {
        this.format = format;
        Form form = request.getResourceRef().getQueryAsForm();
        this.width = 512;
        this.height = 80;
        Parameter width = form.getFirst("width");
        Parameter height = form.getFirst("height");
        if (width != null) {
            this.width = parseInt(width.getValue(), this.width);
        }
        if (height != null) {
            this.height = parseInt(height.getValue(), this.height);
        }
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public PrintngWriter printngWriter(Document document) {
        return new ImageWriter(document, this.width, this.height, format);
    }

}
