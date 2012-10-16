package org.geoserver.printng.restlet;

import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.spi.HtmlWriter;
import org.geoserver.printng.spi.ImageWriter;
import org.geoserver.printng.spi.JSONWriter;
import org.geoserver.printng.spi.PDFWriter;
import org.geoserver.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Variant;

public final class OutputDescriptor {

    private final Variant variant;

    private final PrintngWriter writer;
    
    private final String extension;

    private OutputDescriptor(String extension, Form form) throws RestletException {
        if ("pdf".equalsIgnoreCase(extension)) {
            variant = new Variant(MediaType.APPLICATION_PDF);
            writer = new PDFWriter();
        } else if ("jpg".equalsIgnoreCase(extension)) {
            variant = new Variant(MediaType.IMAGE_JPEG);
            writer = new ImageWriter("jpg");
        } else if ("png".equalsIgnoreCase(extension)) {
            variant = new Variant(MediaType.IMAGE_PNG);
            writer = new ImageWriter("png");
        } else if ("gif".equalsIgnoreCase(extension)) {
            variant = new Variant(MediaType.IMAGE_GIF);
            writer = new ImageWriter("gif");
        } else if ("html".equalsIgnoreCase(extension)) {
            variant = new Variant(MediaType.TEXT_HTML);
            writer = new HtmlWriter();
        } else if ("json".equalsIgnoreCase(extension)) {
            // the json response type requires a format parameter that will
            // drive the actual output format
            String format = form.getFirstValue("format", true);
            if (format == null) {
                throw new RestletException(
                        "json response requires additional 'format' parameter",
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
            variant = new Variant(MediaType.APPLICATION_JSON);
            writer = new JSONWriter(new OutputDescriptor(format, form));
        } else {
            String error = String.format("invalid format \"%s\"", extension);
            throw new RestletException(error, Status.CLIENT_ERROR_BAD_REQUEST);
        }
        this.extension = extension.toLowerCase();
    }
    
    public static OutputDescriptor fromRequest(Request req) {
        String extension = req.getAttributes().get("ext").toString().toLowerCase();
        return new OutputDescriptor(extension, req.getResourceRef().getQueryAsForm());
    }

    public String getExtension() {
        return extension;
    }
    
    public Variant getVariant() {
        return variant;
    }

    public PrintngWriter getWriter() {
        return writer;
    }

}
