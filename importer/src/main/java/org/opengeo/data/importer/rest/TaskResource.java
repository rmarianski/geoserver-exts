package org.opengeo.data.importer.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.Importer;
import org.restlet.data.*;
import org.restlet.ext.fileupload.RestletFileUpload;

/**
 * REST resource for /imports/<import>/tasks[/<id>]
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class TaskResource extends AbstractResource {

    Importer importer;

    public TaskResource(Importer importer) {
        this.importer = importer;
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new ImportTaskJSONFormat());
    }

    @Override
    public void handleGet() {
        Object obj = lookupTask(true);
        if (obj instanceof ImportTask) {
            getResponse().setEntity(getFormatGet().toRepresentation((ImportTask)obj));
        }
        else {
            getResponse().setEntity(getFormatGet().toRepresentation((List<ImportTask>)obj));
        }
    }

    public boolean allowPost() {
        return getAttribute("task") == null;
    }

    public void handlePost() {
        ImportData data = null;
        
        getLogger().info("Handling POST of " + getRequest().getEntity().getMediaType());
        //file posted from form
        MediaType mimeType = getRequest().getEntity().getMediaType(); 
        if (MediaType.MULTIPART_FORM_DATA.equals(mimeType, true)) {
            data = handleMultiPartFormUpload();
        }
        else if (MediaType.APPLICATION_WWW_FORM.equals(mimeType, true)) {
            data = handleFormPost();
        }

        if (data == null) {
            throw new RestletException("Unsupported POST", Status.CLIENT_ERROR_FORBIDDEN);
        }

        acceptData(data);
    }

    private void acceptData(ImportData data) {
        ImportContext context = lookupContext();
        List<ImportTask> newTasks = null;
        try {
            newTasks = importer.update(context, data);
            //importer.prep(context);
            //context.updated();
        } 
        catch (IOException e) {
            throw new RestletException("Error updating context", Status.SERVER_ERROR_INTERNAL, e);
        }

        if (!newTasks.isEmpty()) {
            Object result = newTasks;
            if (newTasks.size() == 1) {
                result = newTasks.get(0);
                long taskId = newTasks.get(0).getId();
                getResponse().redirectSeeOther(getPageInfo().rootURI(
                    String.format("/imports/%d/tasks/%d", context.getId(), taskId)));
            }

            getResponse().setEntity(new ImportTaskJSONFormat().toRepresentation(result));
            getResponse().setStatus(Status.SUCCESS_CREATED);
        }

    }

    private Directory createDirectory() {
        try {
            return Directory.createNew(importer.getCatalog().getResourceLoader().findOrCreateDirectory("uploads"));
        } catch (IOException ioe) {
            throw new RestletException("File upload failed", Status.SERVER_ERROR_INTERNAL, ioe);
        }
    }
    
    private ImportData handleFileUpload() {
        Directory directory = createDirectory();
        
        try {
            directory.accept(getAttribute("task"),getRequest().getEntity().getStream());
        } catch (IOException e) {
            throw new RestletException("Error unpacking file", 
                Status.SERVER_ERROR_INTERNAL, e);
        }
        
        return directory;
    }
    
    private ImportData handleMultiPartFormUpload() {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // @revisit - this appears to be causing OOME
        //factory.setSizeThreshold(102400000);

        RestletFileUpload upload = new RestletFileUpload(factory);
        List<FileItem> items = null;
        try {
            items = upload.parseRequest(getRequest());
        } catch (FileUploadException e) {
            throw new RestletException("File upload failed", Status.SERVER_ERROR_INTERNAL, e);
        }

        //create a directory to hold the files
        Directory directory = createDirectory();

        //unpack all the files
        for (FileItem item : items) {
            if (item.getName() == null) {
                continue;
            }
            try {
                directory.accept(item);
            } catch (Exception ex) {
                throw new RestletException("Error writing file " + item.getName(), Status.SERVER_ERROR_INTERNAL, ex);
            }
        }
        return directory;
    }

    public boolean allowPut() {
        return getAttribute("task") != null;
    }

    public void handlePut() {
        if (getRequest().getEntity().getMediaType().equals(MediaType.APPLICATION_JSON)) {
            handleTaskPut();
        } else {
            acceptData(handleFileUpload());
        }
    }

    ImportContext lookupContext() {
        long i = Long.parseLong(getAttribute("import"));

        ImportContext context = importer.getContext(i);
        if (context == null) {
            throw new RestletException("No such import: " + i, Status.CLIENT_ERROR_NOT_FOUND);
        }
        return context;
    }

    Object lookupTask(boolean allowAll) {
        ImportContext context = lookupContext();

        String t = getAttribute("task");
        if (t != null) {
            int id = Integer.parseInt(t);
            if (id >= context.getTasks().size()) {
                throw new RestletException("No such task: " + id + " for import: " + context.getId(),
                    Status.CLIENT_ERROR_NOT_FOUND);
            }

            return context.getTasks().get(id);
        }
        else {
            if (allowAll) {
                return context.getTasks();
            }
            throw new RestletException("No task specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    void handleTaskPut() {        
        ImportTask task = (ImportTask) getFormatPostOrPut().toObject(getRequest().getEntity());
        ImportTask orig = (ImportTask) lookupTask(false);
        
        boolean change = false;
        if (task.getStore() != null) {
            updateStoreInfo(orig, task.getStore());
            change = true;
        }
        if (task.getData() != null) {
            orig.getData().setCharsetEncoding(task.getData().getCharsetEncoding());
            change = true;
        }
        if (task.getUpdateMode() != null) {
            orig.setUpdateMode(task.getUpdateMode());
            change = true;
        }
        
        if (!change) {
            throw new RestletException("Unknown representation", Status.CLIENT_ERROR_BAD_REQUEST);
        } else {
            importer.changed(orig);
            getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
        }
    }
    
    void updateStoreInfo(ImportTask orig, StoreInfo update) {
        // allow an existing store to be referenced as the target
        StoreInfo newTargetRequested = (StoreInfo) update;
        StoreInfo existing = orig.getStore();
        
        if (existing == null) {
            assert existing != null : "Expected existing store";
        }
        Class storeType = existing instanceof DataStoreInfo
                ? DataStoreInfo.class : null;
        if (storeType == null) {
            assert storeType != null : "Cannot handle " + existing.getClass();
        }
        
        StoreInfo requestedExisting = importer.getCatalog().getStoreByName(
                newTargetRequested.getWorkspace(), 
                newTargetRequested.getName(), 
                storeType);
        
        if (requestedExisting != null && storeType == DataStoreInfo.class) {
            CatalogBuilder cb = new CatalogBuilder(importer.getCatalog());
            DataStoreInfo clone = cb.buildDataStore(requestedExisting.getName());
            cb.updateDataStore(clone, (DataStoreInfo) requestedExisting);
            ((StoreInfoImpl) clone).setId(requestedExisting.getId());
            orig.setStore(clone);
            orig.setDirect(false);
        } else {
            throw new RestletException("Can only set target to existing datastore", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    private ImportData handleFormPost() {
        Form form = getRequest().getEntityAsForm();
        String url = form.getFirstValue("url", null);
        if (url == null) {
            throw new RestletException("Invalid request", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        URL location = null;
        try {
            location = new URL(url);
        } catch (MalformedURLException ex) {
            getLogger().warning("invalid URL specified in upload : " + url);
        }
        // @todo handling remote URL implies asynchronous processing at this stage
        if (location == null || !location.getProtocol().equalsIgnoreCase("file")) {
            throw new RestletException("Invalid url in request", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        File file;
        try {
            file = new File(location.toURI().getPath());
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Unexpected exception", ex);
        }
        
        Directory dir;
        if (file.isDirectory()) {
            dir = new Directory(file);
        } else {
            dir = new Directory(file.getParentFile());
            try {
                dir.unpack(file);
            } catch (IOException ioe) {
                getLogger().log(Level.WARNING, "Error unpacking " + file.getAbsolutePath(), ioe);
                throw new RestletException("Possible invalid file", Status.SERVER_ERROR_INTERNAL);
            }
        }
        return dir;
    }

    class ImportTaskJSONFormat extends StreamDataFormat {

        ImportTaskJSONFormat() {
            super(MediaType.APPLICATION_JSON);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            ImportJSONIO json = new ImportJSONIO(importer);
            
            return json.task(in);
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            ImportJSONIO json = new ImportJSONIO(importer);

            if (object instanceof ImportTask) {
                ImportTask task = (ImportTask) object;
                json.task(task, getPageInfo(), out);
            }
            else {
                json.tasks((List<ImportTask>)object, getPageInfo(), out);
            }
        }

    }
}
