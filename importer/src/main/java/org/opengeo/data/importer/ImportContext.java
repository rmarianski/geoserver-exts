package org.opengeo.data.importer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Maintains state about an import.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ImportContext implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 8790675013874051197L;

    public static enum State {
        PENDING, READY, RUNNING, INCOMPLETE, COMPLETE
    }

    /** identifier */
    Long id;

    /** state */
    State state = State.PENDING;

    /**
     * data source
     */
    ImportData data;

    /**
     * target workspace for the import 
     */
    WorkspaceInfo targetWorkspace;

    /**
     * target store of the import
     */
    StoreInfo targetStore;

    /** 
     * import tasks
     */
    List<ImportTask> tasks = new ArrayList<ImportTask>();

    /** 
     * id generator for task 
     */
    int taskid = 0;

    /**
     * date import was created
     */
    Date created;

    /**
     * date import was finished
     */
    Date updated;

    /**
     * credentials of creator
     */
    String user;

    public ImportContext(long id) {
        this();
        this.id = id;
    }

    public ImportContext() {
        this.created = new Date();
        this.updated = new Date(created.getTime());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public ImportData getData() {
        return data;
    }

    public void setData(ImportData data) {
        this.data = data;
    }

    public WorkspaceInfo getTargetWorkspace() {
        return targetWorkspace;
    }

    public void setTargetWorkspace(WorkspaceInfo targetWorkspace) {
        this.targetWorkspace = targetWorkspace;
    }

    public StoreInfo getTargetStore() {
        return targetStore;
    }

    public void setTargetStore(StoreInfo targetStore) {
        this.targetStore = targetStore;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
    
    public List<ImportTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void addTask(ImportTask task) {
        task.setId(taskid++);
        task.setContext(this);
        this.tasks.add(task);
    }

    public ImportTask task(long id) {
        for (ImportTask t : tasks) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    public void updateState() {
        State newState = State.COMPLETE;
     O: for (ImportTask task : tasks) {
            switch(task.getState()) {
                case PENDING:
                case RUNNING:
                case INCOMPLETE:
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

    public void updated() {
        updated = new Date();
    }

    public void delete() throws IOException {
        if (data != null) {
            data.cleanup();
        }
    }

    public void reattach() {
        if (data != null) {
            data.reattach();
        }

        for (ImportTask task : tasks) {
            task.setContext(this);
            task.reattach();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        ImportContext other = (ImportContext) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    private Object readResolve() {
        if (tasks == null) {
            tasks = new ArrayList();
        }
        return this;
    }
}
    