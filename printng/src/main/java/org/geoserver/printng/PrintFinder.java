package org.geoserver.printng;

import org.geoserver.rest.format.MediaTypes;
import org.restlet.Finder;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintFinder extends Finder {
    
    public PrintFinder() {
        MediaTypes.registerExtension("pdf", MediaType.APPLICATION_PDF);
        MediaTypes.registerExtension("jpg", MediaType.IMAGE_JPEG);
        MediaTypes.registerExtension("png", MediaType.IMAGE_PNG);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        // @todo ugly
        if (request.getResourceRef().getPath().indexOf("/printng/render") >=0) {
            return new HTMLMapPrintResource(request, response);
        } else {
            return new TemplatePrintResource(request,response);
        }
    }
}
