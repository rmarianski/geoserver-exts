package org.geoserver.printng;

import org.geoserver.printng.io.PrintngReaderFactory;
import org.geoserver.printng.io.PrintngWriterFactory;
import org.geoserver.printng.resource.PrintResource;
import org.geoserver.rest.format.MediaTypes;
import org.restlet.Finder;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
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
            // TODO parse the variant here
            // TODO also instantiate the correct writer factory
            Object object = request.getAttributes().get("ext");
            Variant variant = new Variant(MediaType.IMAGE_PNG);
//            PrintngWriterFactory pwf = new ImagePrintngFactory();
            PrintngWriterFactory pwf = new PDFPrintngFactory();
            PrintResource resource = new PrintResource(request, response, variant, prf, pwf);
            return resource;
        }
    }
}
