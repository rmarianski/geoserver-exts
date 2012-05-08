package org.geoserver.uploader;

import java.io.File;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.util.logging.Logging;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * @author groldan
 * 
 */
public class UploadRecoveryFinder extends Finder {

    private static final Logger LOGGER = Logging.getLogger(UploadRecoveryFinder.class);

    private UploadLifeCyleManager lifeCycleManager;

    private Catalog catalog;

    private UploaderConfigPersister configPersister;

    public UploadRecoveryFinder(Catalog catalog, UploadLifeCyleManager lifeCycleManager,
            UploaderConfigPersister configPersister) {
        this.catalog = catalog;
        this.lifeCycleManager = lifeCycleManager;
        this.configPersister = configPersister;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        String token = RESTUtils.getAttribute(request, "token");

        Resource resource;
        if (token == null) {
            resource = new ResourceUploaderResource(catalog, lifeCycleManager, configPersister);
        } else {

            File pendingUploadDir = lifeCycleManager.getPendingUploadDir(token);
            if (!pendingUploadDir.exists()) {
                throw new RestletException("No pending upload exist for the provided identifier",
                        Status.CLIENT_ERROR_NOT_FOUND);
            }
            resource = new UploadRecoveryResource(catalog, lifeCycleManager, configPersister);
        }
        resource.setRequest(request);
        resource.setResponse(response);
        return resource;
    }

}
