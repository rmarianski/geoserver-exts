package org.geoserver.printng;

import org.geoserver.printng.iface.PrintngReaderFactory;
import org.geoserver.printng.iface.PrintngWriterFactory;
import org.geoserver.printng.resource.PrintResource;
import org.geoserver.printng.writer.ImagePrintngFactory;
import org.geoserver.printng.writer.PDFPrintngFactory;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.MediaTypes;
import org.restlet.Finder;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

/**
 * 
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintFinder extends Finder {

    private final PrintngReaderFactory prf;

    public PrintFinder() {
        this(null);
    }

    // gets set from spring
    public PrintFinder(PrintngReaderFactory readerFactory) {
        this.prf = readerFactory;
        MediaTypes.registerExtension("pdf", MediaType.APPLICATION_PDF);
        MediaTypes.registerExtension("jpg", MediaType.IMAGE_JPEG);
        MediaTypes.registerExtension("png", MediaType.IMAGE_PNG);
        MediaTypes.registerExtension("gif", MediaType.IMAGE_GIF);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        if (prf == null) {
            if (request.getResourceRef().getPath().indexOf("/printng/render") >= 0) {
                return new HTMLMapPrintResource(request, response);
            } else {
                return new TemplatePrintResource(request, response);
            }
        } else {
            Variant variant;
            PrintngWriterFactory pwf;
            String ext = request.getAttributes().get("ext").toString().toLowerCase();
            if ("pdf".equals(ext)) {
                variant = new Variant(MediaType.APPLICATION_PDF);
                pwf = new PDFPrintngFactory(request);
            } else if ("jpg".equals(ext)) {
                variant = new Variant(MediaType.IMAGE_JPEG);
                pwf = new ImagePrintngFactory(request, "jpg");
            } else if ("png".equals(ext)) {
                variant = new Variant(MediaType.IMAGE_PNG);
                pwf = new ImagePrintngFactory(request, "png");
            } else if ("gif".equals(ext)) {
                variant = new Variant(MediaType.IMAGE_GIF);
                pwf = new ImagePrintngFactory(request, "gif");
            } else {
                String error = String.format("Unknown rendering extension \"%s\"", ext);
                throw new RestletException(error, Status.CLIENT_ERROR_NOT_FOUND);
            }
            return new PrintResource(request, response, variant, prf, pwf);
        }
    }
}
