package org.opengeo.data.csv;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.util.KVP;

public class CSVDataStoreFactory implements FileDataStoreFactorySpi {

    private static final String FILE_TYPE = "csv";

    private static final String EXTENSION = "." + FILE_TYPE;

    public static final String[] EXTENSIONS = new String[] { EXTENSION };

    public static final Param FILE_PARAM = new Param("file", File.class, FILE_TYPE + " file", true,
            null, new KVP(Param.EXT, FILE_TYPE));

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

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        try {
            File file = (File) FILE_PARAM.lookUp(params);
            if (file != null) {
                return file.getPath().toLowerCase().endsWith(EXTENSION);
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

    private FileDataStore createDataStoreFromFile(File file) throws IOException {
        return new CSVDataStore(file);
    }

    @Override
    public FileDataStore createDataStore(Map<String, Serializable> params) throws IOException {
        File file = (File) FILE_PARAM.lookUp(params);
        return createDataStoreFromFile(file);
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
        return url.getFile().toLowerCase().endsWith(EXTENSION);
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
