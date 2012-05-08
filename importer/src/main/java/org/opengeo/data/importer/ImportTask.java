package org.opengeo.data.importer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.catalog.StoreInfo;
import org.geoserver.ows.util.OwsUtils;

/**
 * A unit of work during an import.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class ImportTask implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static enum State {
        PENDING, READY, RUNNING, INCOMPLETE, COMPLETE
    }

    /**
     * task id
     */
    long id;

    /**
     * the context this task is part of
     */
    ImportContext context;

    /**
     * source of data for the import
     */
    ImportData data;

    /**
     * The target store for the import 
     */
    StoreInfo store;

    /**
     * Resources created during this import 
     */
    List<ImportItem> items = new ArrayList<ImportItem>();

    /** 
     * state
     */
    State state = State.PENDING;

    /**
     * id generator for items
     */
    int itemid = 0;

    /**
     * flag signalling direct/indirect import
     */
    boolean direct;
    
    UpdateMode updateMode;

    public ImportTask() {
    }

    public ImportTask(ImportData data) {
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ImportContext getContext() {
        return context;
    }

    public void setContext(ImportContext context) {
        this.context = context;
    }

    public ImportData getData() {
        return data;
    }

    public void setData(ImportData data) {
        this.data = data;
    }

    public void setStore(StoreInfo store) {
        this.store = store;
    }

    public StoreInfo getStore() {
        return store;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public List<ImportItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(ImportItem item) {
        item.setId(itemid++);
        item.setTask(this);
        this.items.add(item);
    }

    public void removeItem(ImportItem item) {
        this.items.remove(item);
    }

    public ImportItem item(long id) {
        for (ImportItem item : items) {
            if (id == item.getId()) {
                return item;
            }
        }
        return null;
    }

    /**
     * @deprecated
     */
    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    /**
     * @deprecated
     */
    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }
    
    public void updateState() {
        State newState = State.COMPLETE;
     O: for (ImportItem item : items) {
           switch(item.getState()) {
               case PENDING:
               case RUNNING:
               case ERROR:
               case NO_CRS:
               case NO_BOUNDS:
                   newState = State.INCOMPLETE;
                   break O;
               case COMPLETE:
                   continue;
               case READY:
                   newState = State.READY; 
           }
        }
        state = newState;
    }

    public void reattach() {
        if (getStore() != null) {
            OwsUtils.resolveCollections(getStore());
        }
        for (ImportItem item : items) {
            item.setTask(this);
            item.reattach();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
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
        ImportTask other = (ImportTask) obj;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (id != other.id)
            return false;
        return true;
    }

    private Object readResolve() {
        if (items == null) {
            items = new ArrayList();
        }
        return this;
    }
}
