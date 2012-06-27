package org.geoserver.printng;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.MediaTypes;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

/**
 * Simple prototype of a template print resource - templates go in data_dir/printing2/templates. Any request
 * parameters are passed to the template to render. In this case, there is not much dynamic ability.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class TemplatePrintResource extends PrintResource {
    
    File template;
    
    static final String templatePath = "printing2/templates";

    TemplatePrintResource(Request req, Response resp) {
        super(req, resp);
    }

    @Override
    public Representation getRepresentation(Variant variant) {
        SimpleHash model = createModel();
        MediaType mediaType = variant.getMediaType();
        // @todo cache directory ?
        GeoServerDataDirectory dataDir = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        File templateDir;
        try {
            templateDir = dataDir.findFile(templatePath);
        } catch (IOException ex) {
            throw new RestletException("Error getting data dir subdirectories", Status.SERVER_ERROR_INTERNAL,ex);
        }
        RenderingSupport renderer = renderingSupport();
        try {
            renderer.processTemplate(templateDir, template.getName(), model);
        } catch (IOException ex) {
            throw new RestletException("Template error", Status.SERVER_ERROR_INTERNAL,ex);
        } catch (TemplateException ex) {
            throw new RestletException("Template error", Status.SERVER_ERROR_INTERNAL,ex);
        }
        try {
            renderer.parseTemplate();
        } catch (IOException ex) {
            throw new RestletException("Template error", Status.SERVER_ERROR_INTERNAL,ex);
        } catch (TemplateException ex) {
            throw new RestletException("Template error", Status.SERVER_ERROR_INTERNAL,ex);
        }
        Representation rep;
        if (mediaType == MediaType.TEXT_HTML) {
            rep = new StringRepresentation(renderer.getTemplateOutput(), MediaType.TEXT_HTML);
        } else if (mediaType == MediaType.APPLICATION_PDF) {
            rep = getPDFRepresentation();
        } else {
            String ext = MediaTypes.getExtensionForMediaType(mediaType);
            // @todo hard-coded width/height
            rep = getImageRepresentation(mediaType,ext,800,600);
        }
        return rep;
    }

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);

        String map = (String) request.getAttributes().get("map");
        String type = (String) request.getAttributes().get("type");
        map = initVariants(map,MediaType.TEXT_HTML);
        resolveTemplate(map);
        if (template == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        }
    }

    private void resolveTemplate(String name) {
        GeoServerDataDirectory dataDir = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        try {
            template = dataDir.findFile(templatePath, name + ".ftl");
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RestletException("Error resolving template", Status.SERVER_ERROR_INTERNAL);
        }
    }

    private SimpleHash createModel() {
        SimpleHash hash = new SimpleHash();
        Form form = getRequest().getResourceRef().getQueryAsForm();
        for (String n:form.getNames()) {
            hash.put(n, form.getValues(n));
        }
        return hash;
    }

}
