package org.geoserver.uploader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.logging.Logging;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.Resource;

/**
 * REST end point to take care of uploading spatial data files through an HTML POST form; see
 * package documentation for more details.
 * 
 * @author groldan
 * 
 */
public class ResourceUploaderResource extends Resource {

    private static final Logger LOGGER = Logging.getLogger(ResourceUploaderResource.class);

    private Catalog catalog;

    private UploadLifeCyleManager lifeCycleManager;

    /**
     * Used to get the default upload workspace and datastore dynamically as it may change over the
     * course of the application
     */
    private UploaderConfigPersister configPersister;

    public ResourceUploaderResource(Catalog catalog, UploadLifeCyleManager lifeCycleManager,
            UploaderConfigPersister configPersister) {
        this.catalog = catalog;
        this.lifeCycleManager = lifeCycleManager;
        this.configPersister = configPersister;
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    /**
     * 
     * @param request
     * @param response
     */
    @Override
    public void handlePost() {
        final Request request = getRequest();
        final Response response = getResponse();
        final MediaType requestMediaType = request.getEntity().getMediaType();
        final boolean ignoreParameters = true;
        if (!MediaType.MULTIPART_FORM_DATA.equals(requestMediaType, ignoreParameters)) {
            response.setStatus(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        JSONObject result = new JSONObject();

        try {
            JSONObject tmpresult = new JSONObject();
            tmpresult.put("success", Boolean.TRUE);

            List<LayerInfo> importedLayers = uploadLayers(request, response);

            for (LayerInfo importedLayer : importedLayers) {
                JSONObject layerResult = new JSONObject();

                final String qname = importedLayer.getResource().getPrefixedName();
                final String encodedQname = ResponseUtils.urlEncode(qname);
                layerResult.put("name", qname);

                String uri = RESTUtils.getBaseURL(request);
                uri = ResponseUtils.appendPath(uri, "layers", encodedQname + ".json");
                layerResult.put("href", uri);

                tmpresult.append("layers", layerResult);
            }

            result = tmpresult;

            response.setStatus(Status.SUCCESS_OK);

        } catch (MissingInformationException e) {
            String token = e.getToken();
            response.setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED);
            addErrors(result, e.getLocator(), e, token);
        } catch (InvalidParameterException e) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            addErrors(result, e.getLocator(), e, null);
        } catch (RuntimeException e) {
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            addErrors(result, "Internal Error", e, null);
        } catch (JSONException e) {
            // shouldn't happen
            throw new RestletException("JSON error", Status.SERVER_ERROR_INTERNAL, e);
        }

        String responseContents = result.toString();
        response.setEntity(responseContents, MediaType.TEXT_HTML);
    }

    /**
     * errors format, {errors: [{id: "file", msg: "invalid"}], trace: "foo"} would work
     */
    private void addErrors(final JSONObject result, final String locator, final Exception ex,
            String uploadToken) {

        Throwable cause = ex;
        try {
            result.put("success", Boolean.FALSE);
            while (cause != null) {
                String message = cause.getMessage();
                if (message != null && message.trim().length() != 0) {
                    JSONObject jsonErr = new JSONObject();
                    jsonErr.put("id", locator);
                    jsonErr.put("msg", message);
                    result.append("errors", jsonErr);
                }
                cause = cause.getCause();
            }
            if (uploadToken != null) {
                result.put("token", uploadToken);
            }
        } catch (JSONException e) {
            // shouldn't happen
            throw new RestletException("JSON error", Status.SERVER_ERROR_INTERNAL, e);
        }
    }

    private List<LayerInfo> uploadLayers(Request request, Response response)
            throws InvalidParameterException, MissingInformationException {
        RestletFileUpload rfu = new RestletFileUpload();
        DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
        rfu.setFileItemFactory(fileItemFactory);
        List<FileItem> items;
        try {
            items = rfu.parseRequest(request);
        } catch (FileUploadException e) {
            throw new RuntimeException("Unknown error parsing the request", e);
        }
        Map<String, Object> params = getRequestParams(items);

        return uploadLayers(params);
    }

    /**
     * Imports the layers comming from a POST form into the GeoServer catalog.
     * <p>
     * The following parameters are expected in {@code params}
     * <ul>
     * <li>{@code file}: Mandatory. {@link FileItem} containing the uploaded file
     * <li>{@code workspace}: Optional: workspace name where to import the spatial data file(s)
     * contained in {@code file}
     * <li>{@code store}: Optional: target store name (belonging to {@code workspace} if provided,
     * or to the uploader's default workspace otherwise). If not provided uploaded resources are
     * kept in its original format under the GeoServer's data directory.
     * <li>{@code title}: Optional: uploaded resource title
     * <li>{@code abstract}: Optional: uploaded resource abstract
     * </ul>
     * </p>
     * 
     * @param params
     * @return
     * @throws MissingInformationException
     * @throws IOException
     * @throws Exception
     */
    public List<LayerInfo> uploadLayers(Map<String, Object> params)
            throws InvalidParameterException, MissingInformationException {
        FileItem fileItem = (FileItem) params.get("file");
        if (fileItem == null) {
            throw new InvalidParameterException("file",
                    "Expected a 'file' parameter that was not provided");
        }
        final String fileItemName = fileItem.getName();
        final File targetDirectory = lifeCycleManager.createTargetDirectory(fileItemName);

        List<LayerInfo> importedLayers = new ArrayList<LayerInfo>();

        try {
            LayerInfo importedLayer;
            List<File> spatialFiles = doFileUpload(fileItem, targetDirectory);
            final WorkspaceInfo targetWorkspace = getTargetWorkspace(params);
            final DataStoreInfo targetDataStore = getTargetDataStore(targetWorkspace, params);

            for (File spatialFile : spatialFiles) {
                LayerUploader importer = findImporter(targetWorkspace, targetDataStore, spatialFile);
                importer.setTitle((String) params.get("title"));
                importer.setAbstract((String) params.get("abstract"));

                try {
                    importedLayer = importer.importFromFile(spatialFile);
                } catch (MissingInformationException e) {
                    String savedToken = lifeCycleManager.saveAsPending(targetDirectory);
                    e.setToken(savedToken);
                    throw e;
                }
                importedLayers.add(importedLayer);
            }

            boolean canDeleteUploadedFiles = targetDataStore != null;
            if (canDeleteUploadedFiles) {
                lifeCycleManager.deleteTargetDirectory(targetDirectory);
            }
        } catch (RuntimeException e) {
            lifeCycleManager.deleteTargetDirectory(targetDirectory);
            throw e;
        } finally {
            try {
                fileItem.delete();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "", e);
            }
        }
        return importedLayers;
    }

    private List<File> doFileUpload(FileItem fileItem, final File targetDirectory) {
        final File uploaded = new File(targetDirectory, fileItem.getName());

        try {
            fileItem.write(uploaded);
        } catch (Exception e) {
            throw new RuntimeException("Error writing uploaded file to disk", e);
        }

        VFSWorker vfs = new VFSWorker();
        if (vfs.canHandle(uploaded)) {
            try {
                vfs.extractTo(uploaded, targetDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Error uncompressing uploaded archive", e);
            }
            uploaded.delete();
        }

        List<File> spatialFiles;
        spatialFiles = findSpatialFile(targetDirectory);
        if (spatialFiles.size() == 0) {
            throw new InvalidParameterException("file", "The file provided is not supported.");
        }

        return spatialFiles;
    }

    private LayerUploader findImporter(final WorkspaceInfo targetWorkspace,
            final DataStoreInfo targetDataStore, File spatialFile) {
        LayerUploader importer;
        if (FeatureTypeUploader.canHandle(spatialFile)) {
            importer = new FeatureTypeUploader(catalog, targetWorkspace, targetDataStore);
        } else if (CoverageUploader.canHandle(spatialFile)) {
            importer = new CoverageUploader(catalog, targetWorkspace);
        } else {
            throw new InvalidParameterException("file", "The file provided is not supported");
        }
        return importer;
    }

    /**
     * Returns the target workspace in the following precedence order:
     * <ul>
     * <li>If one is specified by the user through the "workspace" parameter, that one is returned,
     * at least it doesn't exist, in which case an exception is thrown.
     * <li>The uploader's {@link UploaderConfig#getDefaultWorkspace() default} workspace, if set.
     * <li>GeoServer's default workspace.
     * <li>
     * 
     * @param params
     * @return
     */
    private WorkspaceInfo getTargetWorkspace(Map<String, Object> params) {

        final UploaderConfig config = this.configPersister.getConfig();
        final WorkspaceInfo uploaderDefaultWorkspace = config.defaultWorkspace();

        String workspaceId = (String) params.get("workspace");
        WorkspaceInfo workspaceInfo;
        if (null == workspaceId || workspaceId.trim().length() == 0) {
            if (uploaderDefaultWorkspace == null) {
                workspaceInfo = catalog.getDefaultWorkspace();
                if (workspaceInfo == null) {
                    throw new InvalidParameterException("workspace",
                            "There's no default workspace. "
                                    + "Create a Workspace before uploading data");
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Using GeoServer's default workspace " + workspaceInfo.getName()
                            + " to upload " + params);
                }
            } else {
                workspaceInfo = uploaderDefaultWorkspace;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Using uploader's configured default workspace:"
                            + workspaceInfo.getName() + " to upload " + params);
                }
            }
        } else {
            workspaceInfo = catalog.getWorkspaceByName(workspaceId);
            if (null == workspaceInfo) {
                throw new InvalidParameterException("workspace",
                        "The provided workspace does not exist: " + workspaceId);
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Using user requested workspace:" + workspaceInfo.getName()
                        + " to upload " + params);
            }
        }
        return workspaceInfo;
    }

    private DataStoreInfo getTargetDataStore(WorkspaceInfo targetWorkspace,
            Map<String, Object> params) {

        final UploaderConfig config = this.configPersister.getConfig();
        final DataStoreInfo uploaderDefaultDataStore = config.defaultDataStore();

        DataStoreInfo storeInfo = getRequestedDataStore(targetWorkspace, params);
        if (storeInfo == null) {
            storeInfo = uploaderDefaultDataStore;
        }
        if (storeInfo != null) {
            ResourcePool resourcePool = catalog.getResourcePool();
            DataAccess<? extends FeatureType, ? extends Feature> dataStore;
            DataAccessFactory dsFac;
            try {
                dataStore = storeInfo.getDataStore(null);
                dsFac = resourcePool.getDataStoreFactory(storeInfo);
            } catch (IOException e) {
                throw new RuntimeException("Could not aquire a handle to the provided store "
                        + targetWorkspace.getName() + ":" + storeInfo.getName(), e);
            }
            boolean valid = dsFac instanceof JDBCDataStoreFactory;
            valid |= dataStore instanceof DirectoryDataStore;
            String displayName = dsFac.getDisplayName();
            valid |= displayName.toLowerCase().contains("arcsde");
            if (!valid) {
                throw new InvalidParameterException("store", "Target DataStore "
                        + storeInfo.getWorkspace().getName() + ":" + storeInfo.getName()
                        + " is invalid. It is not known to support "
                        + "the creation of new FeatureTypes");
            }
        }
        return storeInfo;
    }

    private DataStoreInfo getRequestedDataStore(WorkspaceInfo targetWorkspace,
            Map<String, Object> params) {
        String storeId = (String) params.get("store");
        StoreInfo storeInfo = null;
        if (null != storeId && storeId.trim().length() > 0) {
            storeInfo = catalog.getStoreByName(targetWorkspace, storeId, StoreInfo.class);
            if (storeInfo == null) {
                throw new InvalidParameterException("store", "Requested store '" + storeId
                        + "' does not exist in workspace '" + targetWorkspace.getName() + "'");
            }
            if (!(storeInfo instanceof DataStoreInfo)) {
                throw new InvalidParameterException(
                        "store",
                        "Specified store '"
                                + storeId
                                + "' at workspace '"
                                + targetWorkspace.getName()
                                + "' is not a DataStore. It is not possible to post to existing CoverageStores.");
            }

        }
        return (DataStoreInfo) storeInfo;
    }

    private List<File> findSpatialFile(File targetDirectory) {

        File[] files = targetDirectory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                }
                boolean canHandle = FeatureTypeUploader.canHandle(pathname)
                        || CoverageUploader.canHandle(pathname);
                return canHandle;
            }
        });

        List<File> spatialFiles = new ArrayList<File>(files.length);
        List<File> subdirs = new ArrayList<File>(2);
        for (File f : files) {
            if (f.isDirectory()) {
                subdirs.add(f);
            } else {
                spatialFiles.add(f);
            }
        }
        for (File subdir : subdirs) {
            spatialFiles.addAll(findSpatialFile(subdir));
        }

        return spatialFiles;
    }

    private Map<String, Object> getRequestParams(List<FileItem> items) {
        Map<String, Object> params = new HashMap<String, Object>();
        for (FileItem item : items) {
            if (item.isFormField()) {
                params.put(item.getFieldName(), item.getString());
            } else {
                if (item.getFieldName().equals("file")) {
                    params.put("file", item);
                }
            }
        }
        return params;
    }
}
