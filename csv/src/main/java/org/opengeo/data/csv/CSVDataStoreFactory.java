package org.opengeo.data.csv;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.util.KVP;
import org.opengeo.data.csv.CSVDataStore.StrategyType;

public class CSVDataStoreFactory implements FileDataStoreFactorySpi {

    private static final String FILE_TYPE = "csv";

    public static final String[] EXTENSIONS = new String[] { "." + FILE_TYPE };

    public static final Param FILE_PARAM = new Param("file", File.class, FILE_TYPE + " file", false);

    public static final Param URL_PARAM = new Param("url", URL.class, FILE_TYPE + " file", false);

    public static final Param CONTEXT_PARAM = new Param("context", String.class, "Context", false);

    public static final Param NAMESPACEP = new Param("namespace", URI.class,
            "uri to the namespace", false, null, new KVP(Param.LEVEL, "advanced"));

    public static final Param[] parametersInfo = new Param[] { FILE_PARAM };

    @Override
    public String getDisplayName() {
        return FILE_TYPE.toUpperCase();
    }

    @Override
    public String getDescription() {
        return "Comma delimited text file";
    }

    @Override
    public Param[] getParametersInfo() {
        return parametersInfo;
    }

    private boolean canProcessExtension(String filename) {
        String extension = FilenameUtils.getExtension(filename);
        return FILE_TYPE.equalsIgnoreCase(extension);
    }

    private File fileFromParams(Map<String, Serializable> params) throws IOException {
        File file = (File) FILE_PARAM.lookUp(params);
        if (file != null) {
            return file;
        }
        URL url = (URL) URL_PARAM.lookUp(params);
        if (url != null) {
            return new File(url.getFile());
        }
        return null;
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        try {
            File file = fileFromParams(params);
            if (file != null) {
                return canProcessExtension(file.getPath());
            }
        } catch (IOException e) {
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        try {
            CSVDataStore.class.getName();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public Map<Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }

    public FileDataStore createDataStoreFromFile(File file) throws IOException {
        return createDataStoreFromFile(file, null, StrategyType.GEOMETRY_FROM_LATLNG);
    }

    public FileDataStore createDataStoreFromFile(File file, URI namespace, StrategyType strategyType)
            throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Cannot create store from null file");
        } else if (!file.exists()) {
            throw new IllegalArgumentException("Cannot create store with file that does not exist");
        }
        return new CSVDataStore(file, namespace, strategyType);
    }

    @Override
    public FileDataStore createDataStore(Map<String, Serializable> params) throws IOException {
        File file = fileFromParams(params);
        URI namespace = (URI) NAMESPACEP.lookUp(params);
        Object context = CONTEXT_PARAM.lookUp(params);
        StrategyType strategyType = StrategyType.GEOMETRY_FROM_LATLNG;
        if (context != null) {
            String contextValue = context.toString();
            if (contextValue.equalsIgnoreCase("importer")) {
                strategyType = StrategyType.ONLY_ATTRIBUTES;
            }
        }
        return createDataStoreFromFile(file, namespace, strategyType);
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        return createDataStore(params);
    }

    @Override
    public FileDataStore createDataStore(URL url) throws IOException {
        File file = new File(url.getFile());
        return createDataStoreFromFile(file);
    }

    @Override
    public String[] getFileExtensions() {
        return EXTENSIONS;
    }

    @Override
    public boolean canProcess(URL url) {
        return canProcessExtension(url.getFile());
    }

    @Override
    public String getTypeName(URL url) throws IOException {
        DataStore ds = createDataStore(url);
        String[] names = ds.getTypeNames();
        assert names.length == 1 : "Invalid number of type names for csv file store";
        ds.dispose();
        return names[0];
    }
}
