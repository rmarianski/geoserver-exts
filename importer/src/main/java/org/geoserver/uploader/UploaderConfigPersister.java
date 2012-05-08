package org.geoserver.uploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geotools.util.logging.Logging;

import com.thoughtworks.xstream.XStream;

public class UploaderConfigPersister {

    private static final Logger LOGGER = Logging.getLogger(UploaderConfigPersister.class);

    static final String UPLOADER_CONFIG_FILE_NAME = "uploader.xml";

    private GeoServerDataDirectory dataDir;

    private UploaderConfig config;

    private Catalog catalog;

    public UploaderConfigPersister(Catalog catalog, GeoServerDataDirectory dataDirectory) {
        this.catalog = catalog;
        this.dataDir = dataDirectory;
        try {
            this.config = loadConfig();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading uploader config from "
                    + UPLOADER_CONFIG_FILE_NAME + ". Using GeoServer's defaults", e);
        }
        if (config == null) {
            config = new UploaderConfig(catalog);
            try {
                UploaderConfig sampleConfig = new UploaderConfig(catalog);
                sampleConfig.setDefaultWorkspace("ws_name");
                sampleConfig.setDefaultDataStore("ds_name");
                saveConfig(sampleConfig, UPLOADER_CONFIG_FILE_NAME + ".example");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public UploaderConfig getConfig() {
        return new UploaderConfig(config);
    }

    public void setDefaults(WorkspaceInfo ws, DataStoreInfo ds) {
        config.setDefaultWorkspace(ws == null ? null : ws.getName());
        config.setDefaultDataStore(ds == null ? null : ds.getName());
        try {
            saveConfig();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error persisting uploader configuration: " + e.getMessage(), e);
        }
    }

    private UploaderConfig loadConfig() throws IOException {
        UploaderConfig config = null;
        File file = new File(dataDir.root(), UPLOADER_CONFIG_FILE_NAME);
        if (!file.exists()) {
            return null;
        }
        Persister persister = new Persister();
        InputStream in = new FileInputStream(file);
        try {
            config = persister.load(in, UploaderConfig.class);
            config.setCatalog(catalog);
        } finally {
            in.close();
        }

        return config;
    }

    private void saveConfig() throws IOException {
        String fileName = UPLOADER_CONFIG_FILE_NAME;
        saveConfig(config, fileName);
    }

    private void saveConfig(UploaderConfig config, String fileName) throws IOException {
        File file = new File(dataDir.root(), fileName);
        OutputStream out = new FileOutputStream(file);
        Persister persister = new Persister();
        persister.save(config, out);
    }

    private static class Persister extends XStreamPersister {
        public Persister() {
            super();
        }

        @Override
        protected void init(XStream xs) {
            super.init(xs);
            xs.alias("UploaderConfig", UploaderConfig.class);
        }
    }
}
