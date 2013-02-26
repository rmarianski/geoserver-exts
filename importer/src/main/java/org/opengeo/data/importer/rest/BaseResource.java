package org.opengeo.data.importer.rest;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.Importer;
import org.restlet.data.Status;

public abstract class BaseResource extends AbstractResource {

    protected Importer importer;

    protected BaseResource(Importer importer) {
        this.importer = importer;
    }

    protected ImportContext lookupContext() {
        return lookupContext(false);
    }

    protected ImportContext lookupContext(boolean optional) {
        long i = Long.parseLong(getAttribute("import"));

        ImportContext context = importer.getContext(i);
        if (!optional && context == null) {
            throw new RestletException("No such import: " + i, Status.CLIENT_ERROR_NOT_FOUND);
        }
        return context;
    }
}
