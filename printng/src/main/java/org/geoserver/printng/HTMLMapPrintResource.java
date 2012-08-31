package org.geoserver.printng;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.MediaTypes;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.w3c.dom.Element;

/**
 * Accept a POST'd html transitional map and render to pdf or image. Intended for thumbnail
 * generation, document element attributes of width and height will be used to size output image.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HTMLMapPrintResource extends PrintResource {
    
    RenderingSupport renderer;
    
    HTMLMapPrintResource(Request req, Response resp) {
        super(req, resp);
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }
    
    @Override
    public void handlePost() {
        // @todo probably need to allow specifying what uris should be cached and for how long
        // e.g. openstreetmap world (or other large) area base tiles are valid for a while
        // but overlay layers should be fetched more regularly or not cached at all
        renderer = renderingSupport();
        renderer.disableImageCaching();
        renderer.setAllowXHTMLTransitional(true);
        Request req = getRequest();
        // @todo geoserver2.2 somehow prevents getting media type???
//        if (req.getEntity().getMediaType() == MediaType.TEXT_HTML) {
            Form params = req.getResourceRef().getQueryAsForm();
            String dpp = params.getFirstValue("dpp");
            if (dpp != null) {
                try {
                    renderer.setDotsPerPixel(Integer.parseInt(dpp));
                } catch (NumberFormatException fne) {
                    throw new RestletException("Invalid DPP '" + dpp + "', should be integer number",Status.CLIENT_ERROR_BAD_REQUEST);
                }
            }
            String resource = (String) req.getAttributes().get("resource");
            String uri = "file:///nonexisting";
            if (resource != null) {
                File resourceDir = getGeoserverDirectory(false, "resources", resource);
                getLogger().info("request for printing resources " + resource);
                if (resourceDir == null) {
                    GeoServerDataDirectory geoserverDataDirectory = getGeoserverDataDirectory();
                    File dataDirPath = geoserverDataDirectory.root();
                    getLogger().warning("resources not found at " + dataDirPath.getAbsolutePath());
                    throw new RestletException("Invalid request",Status.CLIENT_ERROR_BAD_REQUEST);
                }
                try {
                    uri = resourceDir.toURI().toURL().toString();
                } catch (MalformedURLException ex) {
                    throw new RuntimeException("Unexpected error");
                }
            }
            try {
                renderer.parseInput(req.getEntity().getStream(),uri);
            } catch (IOException ex) {
                throw new RestletException("Error parsing request",Status.SERVER_ERROR_INTERNAL,ex);
            }
//        } else {
//            getLogger().info("Ignoring request with content type " + req.getEntity().getMediaType());
//            throw new RestletException("Invalid request",Status.CLIENT_ERROR_BAD_REQUEST);
//        }
        String cookie = params.getFirstValue("cookie");
        if (cookie != null) {
            String[] parts = cookie.split(",");
            renderer.addCookie(parts[0], parts[1], parts[2]);
        }
        
        Variant variant = getVariants().get(0);
        String ext = MediaTypes.getExtensionForMediaType(variant.getMediaType());
        Representation rep;
        if ("pdf".equals(ext)) {
            rep = getPDFRepresentation();
        } else {
            Element root = renderer.getDocument().getDocumentElement();
            String width = root.getAttribute("width");
            String height = root.getAttribute("height");
            int w = 512;
            int h = 256;
            if (width.length() > 0) {
                w = Integer.parseInt(width);
            }
            if (height.length() > 0) {
                h = Integer.parseInt(height);
            }
            rep = getImageRepresentation(variant.getMediaType(), ext, w, h);
        }
        getResponse().setEntity(rep);
    }
    
    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        initVariants(request.getResourceRef().getLastSegment(), MediaType.IMAGE_PNG);
    }
    
    
    
}
