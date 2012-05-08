package org.geoserver.uploader;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerTestSupport;

public class UploaderConfigPersisterTest extends GeoServerTestSupport {

    /**
     * This is a READ ONLY TEST (as far as GeoServer is concerned) so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new UploaderConfigPersisterTest());
    }

    @Override
    protected void setUpInternal() {
        try {
            File configFile = getResourceLoader().find(
                    UploaderConfigPersister.UPLOADER_CONFIG_FILE_NAME);
            if (configFile != null && configFile.exists() && !configFile.delete()) {
                throw new IllegalStateException();
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testLoadWhenConfigFileDoesNotExists() {
        UploaderConfigPersister persister = new UploaderConfigPersister(getCatalog(),
                getDataDirectory());
        UploaderConfig config = persister.getConfig();
        assertNotNull(config);
        assertNull(config.getDefaultWorkspace());
        assertNull(config.getDefaultDataStore());
        assertEquals(getCatalog().getDefaultWorkspace(), config.defaultWorkspace());
        assertNull(config.defaultDataStore());
    }

    public void testLoad() throws Exception {
        final WorkspaceInfo ws;
        final DataStoreInfo ds;

        Catalog catalog = getCatalog();
        {
            ws = catalog.getWorkspaceByName("cite");
            assertNotNull(ws);
            assertFalse(ws.equals(catalog.getDefaultWorkspace()));
            ds = catalog.getDataStoresByWorkspace(ws).get(0);
        }
        GeoServerDataDirectory dataDir = getDataDirectory();
        File baseDirectory = dataDir.root();
        File file = new File(baseDirectory, UploaderConfigPersister.UPLOADER_CONFIG_FILE_NAME);

        String contents = "<UploaderConfig><defaultWorkspace>" + ws.getName()
                + "</defaultWorkspace><defaultDataStore>" + ds.getName()
                + "</defaultDataStore></UploaderConfig>";

        FileUtils.writeStringToFile(file, contents);

        UploaderConfigPersister persister;
        persister = new UploaderConfigPersister(catalog, dataDir);
        UploaderConfig config = persister.getConfig();
        assertEquals(ws.getName(), config.getDefaultWorkspace());
        assertEquals(ds.getName(), config.getDefaultDataStore());

        assertEquals(ws, config.defaultWorkspace());
        assertEquals(ds, config.defaultDataStore());
    }

    public void testSave() {
        Catalog catalog = getCatalog();

        UploaderConfigPersister persister;
        persister = new UploaderConfigPersister(catalog, getDataDirectory());
        assertNull(persister.getConfig().getDefaultWorkspace());
        assertNull(persister.getConfig().getDefaultDataStore());

        final WorkspaceInfo ws = catalog.getWorkspaceByName("cite");
        persister.setDefaults(ws, null);

        // force reload
        persister = new UploaderConfigPersister(catalog, getDataDirectory());
        UploaderConfig config;
        config = persister.getConfig();

        assertEquals(ws.getName(), config.getDefaultWorkspace());
        assertNull(config.getDefaultDataStore());

        DataStoreInfo ds = catalog.getDataStoresByWorkspace(ws).get(0);
        assertNotNull(ds);

        persister.setDefaults(ws, ds);

        // force reload
        persister = new UploaderConfigPersister(catalog, getDataDirectory());
        config = persister.getConfig();
        assertEquals(ws.getName(), config.getDefaultWorkspace());
        assertEquals(ds.getName(), config.getDefaultDataStore());
    }
}
