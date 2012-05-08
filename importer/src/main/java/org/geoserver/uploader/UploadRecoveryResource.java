package org.geoserver.uploader;

import java.io.File;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.util.logging.Logging;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * REST end point to take care of uploading spatial data files through an HTML POST form; see
 * package documentation for more details.
 * 
 * @author groldan
 * 
 */
public class UploadRecoveryResource extends Resource {

    private static final Logger LOGGER = Logging.getLogger(UploadRecoveryResource.class);

    private UploadLifeCyleManager lifeCycleManager;

    private Catalog catalog;

    private UploaderConfigPersister configPersister;

    public UploadRecoveryResource(Catalog catalog, UploadLifeCyleManager lifeCycleManager,
            UploaderConfigPersister configPersister) {
        this.catalog = catalog;
        this.lifeCycleManager = lifeCycleManager;
        this.configPersister = configPersister;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        final Request request = getRequest();
        final Response response = getResponse();

        final MediaType requestMediaType = request.getEntity().getMediaType();
        final boolean ignoreParameters = true;
        if (!MediaType.APPLICATION_WWW_FORM.equals(requestMediaType, ignoreParameters)) {
            response.setStatus(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
            response.setEntity("Expected " + MediaType.APPLICATION_WWW_FORM.getName()
                    + " encoded parmeters", MediaType.TEXT_PLAIN);
            return;
        }

        final String pendingUploadToken = RESTUtils.getAttribute(request, "token");
        File pendingUploadDir = lifeCycleManager.startRecovery(pendingUploadToken);
        if (pendingUploadDir == null) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            response.setEntity("Provided pending upload identifier does not exists."
                    + " It may have expired.", MediaType.TEXT_PLAIN);
            return;
        }

    }
}
