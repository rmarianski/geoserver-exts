package org.geoserver.uploader;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Configuration bean for the {@link ResourceUploaderResource}
 * 
 * @author groldan
 * 
 */
public class UploaderConfig {

    public String defaultWorkspace;

    public String defaultDataStore;

    private transient Catalog catalog;

    public UploaderConfig(Catalog catalog) {
        this.catalog = catalog;
    }

    public UploaderConfig(UploaderConfig config) {
        this.catalog = config.catalog;
        this.defaultWorkspace = config.defaultWorkspace;
        this.defaultDataStore = config.defaultDataStore;
    }

    void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public String getDefaultWorkspace() {
        return defaultWorkspace;
    }

    public String getDefaultDataStore() {
        return defaultDataStore;
    }

    public void setDefaultWorkspace(String defaultWorkspace) {
        this.defaultWorkspace = defaultWorkspace;
    }

    public void setDefaultDataStore(String defaultDataStore) {
        this.defaultDataStore = defaultDataStore;
    }

    public WorkspaceInfo defaultWorkspace() {
        WorkspaceInfo workspaceInfo = null;
        if (defaultWorkspace == null) {
            workspaceInfo = catalog.getDefaultWorkspace();
        } else {
            workspaceInfo = catalog.getWorkspaceByName(defaultWorkspace);
        }
        return workspaceInfo;
    }

    public DataStoreInfo defaultDataStore() {
        String defaultDataStore = this.defaultDataStore;
        if (defaultDataStore == null) {
            return null;
        }

        WorkspaceInfo ws = defaultWorkspace();

        if (ws == null) {
            return null;
        }

        DataStoreInfo storeInfo = null;
        storeInfo = catalog.getDataStoreByName(ws.getName(), defaultDataStore);
        return storeInfo;
    }
}
