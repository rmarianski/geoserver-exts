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

    private final PrintngReaderFactory readerFactory;

    private final PrintngWriterFactory writerFactory;

    public PrintFinder() {
        this(null, null);
    }

    // gets set from spring
    public PrintFinder(PrintngReaderFactory readerFactory, PrintngWriterFactory writerFactory) {
        this.readerFactory = readerFactory;
        this.writerFactory = writerFactory;
        MediaTypes.registerExtension("pdf", MediaType.APPLICATION_PDF);
        MediaTypes.registerExtension("jpg", MediaType.IMAGE_JPEG);
        MediaTypes.registerExtension("png", MediaType.IMAGE_PNG);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        if (readerFactory == null || writerFactory == null) {
            if (request.getResourceRef().getPath().indexOf("/printng/render") >= 0) {
                return new HTMLMapPrintResource(request, response);
            } else {
                return new TemplatePrintResource(request, response);
            }
        } else {
            // TODO parse the variant here
            // will probably need to instantiate the appropriate reader and writer factories appropriately
            // or at least the writer factory from the request extension
            Variant variant = new Variant(MediaType.TEXT_HTML);
            PrintResource resource = new PrintResource(request, response, variant, readerFactory,
                    writerFactory);
            return resource;
        }
    }
}
