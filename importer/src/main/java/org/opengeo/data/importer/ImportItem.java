package org.opengeo.data.importer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.ows.util.OwsUtils;
import org.opengeo.data.importer.transform.TransformChain;

/**
 * A resource (feature type, coverage, etc... ) created during an imported.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ImportItem implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static enum State {
        PENDING, READY, RUNNING, NO_CRS, NO_BOUNDS, ERROR, COMPLETE;
    }

    /**
     * item id
     */
    long id;

    /**
     * task this item is part of
     */
    ImportTask task;

    /**
     * the layer/resource
     */
    LayerInfo layer;

    /** 
     * state of the resource
     */
    State state = State.PENDING;

    /**
     * Any error associated with the resource
     */
    Exception error;

    /** 
     * transform to apply to this import item 
     */
    TransformChain transform;

    /**
     * various metadata 
     */
    transient Map<Object,Object> metadata;
    
    String originalName;
    
    transient volatile int totalToProcess;
    
    transient volatile int numberProcessed;
    
    List<LogRecord> importMessages = new ArrayList<LogRecord>();

    /** mode to use when importing into existing dataset */
    UpdateMode updateMode;

    public ImportItem() {
    }

    public ImportItem(LayerInfo layer) {
        this.layer = layer;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ImportTask getTask() {
        return task;
    }

    public void setTask(ImportTask task) {
        this.task = task;
    }

    public LayerInfo getLayer() {
        return layer;
    }

    public void setLayer(LayerInfo layer) {
        this.layer = layer;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    public TransformChain getTransform() {
        return transform;
    }

    public void setTransform(TransformChain transform) {
        this.transform = transform;
    }

    public Map<Object, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<Object, Object>();
        }
        return metadata;
    }
    
    public void clearImportMessages() {
        if (importMessages != null) {
            importMessages.clear();
        }
    }

    public void addImportMessage(Level level,String msg) {
        if (importMessages == null) {
            importMessages = new ArrayList<LogRecord>();
        }
        importMessages.add(new LogRecord(level, msg));
    }
    
    public List<LogRecord> getImportMessages() {
        List<LogRecord> retval;
        if (importMessages == null) {
            retval = Collections.emptyList();
        } else {
            retval = Collections.unmodifiableList(importMessages);
        }
        return retval;
    }

    public String getOriginalName() {
        return originalName == null ? layer.getResource().getNativeName() : originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    public int getNumberProcessed() {
        return numberProcessed;
    }

    public void setNumberProcessed(int numberProcessed) {
        this.numberProcessed = numberProcessed;
    }

    public int getTotalToProcess() {
        return totalToProcess;
    }

    public void setTotalToProcess(int totalToProcess) {
        this.totalToProcess = totalToProcess;
    }

    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    public UpdateMode updateMode() {
        return updateMode != null ? updateMode : task.getUpdateMode();
    }

    public void reattach() {
        if (layer != null) {
            OwsUtils.resolveCollections(layer);
            if (layer.getResource() != null) {
                OwsUtils.resolveCollections(layer.getResource());
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((task == null) ? 0 : task.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImportItem other = (ImportItem) obj;
        if (id != other.id)
            return false;
        if (task == null) {
            if (other.task != null)
                return false;
        } else if (!task.equals(other.task))
            return false;
        return true;
    }
}
