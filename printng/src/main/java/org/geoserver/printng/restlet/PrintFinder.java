package org.geoserver.printng.restlet;

import org.geoserver.printng.api.PrintngReaderFactory;
import org.geoserver.printng.api.PrintngWriterFactory;
import org.geoserver.printng.spi.ImageWriterFactory;
import org.geoserver.printng.spi.PDFWriterFactory;
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
        Variant variant;
        PrintngWriterFactory pwf;
        String ext = request.getAttributes().get("ext").toString().toLowerCase();
        if ("pdf".equals(ext)) {
            variant = new Variant(MediaType.APPLICATION_PDF);
            pwf = new PDFWriterFactory(request);
        } else if ("jpg".equals(ext)) {
            variant = new Variant(MediaType.IMAGE_JPEG);
            pwf = new ImageWriterFactory(request, "jpg");
        } else if ("png".equals(ext)) {
            variant = new Variant(MediaType.IMAGE_PNG);
            pwf = new ImageWriterFactory(request, "png");
        } else if ("gif".equals(ext)) {
            variant = new Variant(MediaType.IMAGE_GIF);
            pwf = new ImageWriterFactory(request, "gif");
        } else {
            String error = String.format("Unknown rendering extension \"%s\"", ext);
            throw new RestletException(error, Status.CLIENT_ERROR_NOT_FOUND);
        }
        return new PrintResource(request, response, variant, prf, pwf);
    }

}
