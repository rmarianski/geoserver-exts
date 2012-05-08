package org.opengeo.data.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.Importer;
import org.opengeo.data.importer.transform.TransformChain;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Representation;

/**
 * REST resource for /imports/<import>/tasks/<task>/items[/<id>]
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ItemResource extends AbstractResource {

    Importer importer;

    public ItemResource(Importer importer) {
        this.importer = importer;
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new ImportItemJSONFormat());
    }

    @Override
    public void handleGet() {
        if (getRequest().getResourceRef().getLastSegment().equals("progress")) {
            getResponse().setEntity(createProgressRepresentation());
        } else {
            getResponse().setEntity(getFormatGet().toRepresentation(lookupItem(true)));
        }
    }
    
    private Representation createProgressRepresentation() {
        JSONObject progress = new JSONObject();
        long imprt = Long.parseLong(getAttribute("import"));
        ImportItem inProgress = importer.getCurrentlyProcessingItem(imprt);
        try {
            if (inProgress != null) {
                progress.put("progress", inProgress.getNumberProcessed());
                progress.put("total", inProgress.getTotalToProcess());
                progress.put("state", inProgress.getState().toString());
            } else {
                ImportItem item = (ImportItem) lookupItem(false);
                progress.put("state", item.getState().toString());
                if (item.getState() == ImportItem.State.ERROR) {
                    if (item.getError() != null) {
                        progress.put("message", item.getError().getMessage());
                    }
                }
            }
        } catch (JSONException jex) {
            throw new RestletException("Internal Error", Status.SERVER_ERROR_INTERNAL, jex);
        }
        return new JsonRepresentation(progress);
    }

    @Override
    public boolean allowPut() {
        return getAttribute("item") != null;
    }

    @Override
    public void handlePut() {
        ImportItem orig = (ImportItem) lookupItem(false);
        ImportItem item = (ImportItem) getFormatPostOrPut().toObject(getRequest().getEntity());

        //update the original layer and resource from the new
        LayerInfo l = item.getLayer();
        ResourceInfo r = l.getResource();
        TransformChain chain = item.getTransform();
        
        //TODO: this is not thread safe, clone the object before overwriting it
        //save the existing resource, which will be overwritten below,  
        ResourceInfo resource = orig.getLayer().getResource();
        
        CatalogBuilder cb = new CatalogBuilder(importer.getCatalog());
        if (l != null) {
            l.setResource(resource);
            // @hack workaround OWSUtils bug - trying to copy null collections
            // why these are null in the first place is a different question
            LayerInfoImpl impl = (LayerInfoImpl) orig.getLayer();
            if (impl.getAuthorityURLs() == null) {
                impl.setAuthorityURLs(new ArrayList(1));
            }
            if (impl.getIdentifiers() == null) {
                impl.setIdentifiers(new ArrayList(1));
            }
            // @endhack
            cb.updateLayer(orig.getLayer(), l);
        }
        
        // validate SRS - an invalid one will destroy capabilities doc and make
        // the layer totally broken in UI
        CoordinateReferenceSystem newRefSystem = null;
        if (r instanceof FeatureTypeInfo) {
            String srs = ((FeatureTypeInfo) r).getSRS();
            if (srs != null) {
                try {
                    newRefSystem = CRS.decode(srs);
                } catch (NoSuchAuthorityCodeException ex) {
                    String msg = "Invalid SRS " + srs;
                    getLogger().warning(msg + " in PUT request");
                    throw ImportJSONIO.badRequest(msg);
                } catch (FactoryException ex) {
                    throw new RestletException("Error with referencing",Status.SERVER_ERROR_INTERNAL,ex);
                }
            }
        }

        //update the resource
        if (r != null) {
            if (r instanceof FeatureTypeInfo) {
                cb.updateFeatureType((FeatureTypeInfo) resource, (FeatureTypeInfo) r);
            }
            else if (r instanceof CoverageInfo) {
                cb.updateCoverage((CoverageInfo) resource, (CoverageInfo) r);
            }
        }
        
        // have to do this after updating the original
        if (newRefSystem != null) {
            try {
                ReferencedEnvelope nativeBounds = cb.getNativeBounds(resource);
                resource.setLatLonBoundingBox(cb.getLatLonBounds(nativeBounds, newRefSystem));
            } catch (IOException ex) {
                throw new RestletException("Error with bounds computation",Status.SERVER_ERROR_INTERNAL,ex);
            }
        }
        
        if (item.getUpdateMode() != null) {
            orig.setUpdateMode(item.getUpdateMode());
        }

        if (chain != null) {
            orig.setTransform(chain);
        }

        //notify the importer that the item has changed
        importer.changed(orig);
        
        getResponse().setStatus(Status.SUCCESS_ACCEPTED);
    }

    public boolean allowDelete() {
        return getAttribute("item") != null;
    }

    public void handleDelete() {
        ImportItem item = (ImportItem) lookupItem(false);
        ImportTask task = item.getTask();
        task.removeItem(item);

        importer.changed(task);
    }

    Object lookupItem(boolean allowAll) {
        long imprt = Long.parseLong(getAttribute("import"));

        ImportContext context = importer.getContext(imprt);
        if (context == null) {
            throw new RestletException("No such import: " + imprt, Status.CLIENT_ERROR_NOT_FOUND);
        }

        int t = Integer.parseInt(getAttribute("task"));
        if (t >= context.getTasks().size()) {
            throw new RestletException("No such task: " + t + " for import: " + imprt,
                    Status.CLIENT_ERROR_NOT_FOUND);
        }

        ImportTask task = context.getTasks().get(t);

        String i = getAttribute("item");
        if (i != null) {
            int id = Integer.parseInt(i);
            if (id >= task.getItems().size()) {
                throw new RestletException("No such item: " + id + " for import: " + imprt + 
                    ", task: " + t, Status.CLIENT_ERROR_NOT_FOUND);
            }

            return task.getItems().get(id);
        }
        else {
            if (allowAll) {
                return task.getItems();
            }
            throw new RestletException("No item specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    class ImportItemJSONFormat extends StreamDataFormat {

        ImportItemJSONFormat() {
            super(MediaType.APPLICATION_JSON);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return new ImportJSONIO(importer).item(in);
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            ImportJSONIO json = new ImportJSONIO(importer);

            if (object instanceof ImportItem) {
                ImportItem item = (ImportItem) object;
                json.item(item, getPageInfo(), out);
            }
            else {
                json.items((List<ImportItem>)object, getPageInfo(), out);
            }
        }

    }
}
