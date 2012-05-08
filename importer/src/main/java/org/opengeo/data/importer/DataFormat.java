package org.opengeo.data.importer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 * Represents a type of data and encapsulates I/O operations.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */

public abstract class DataFormat implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * mappings of file name extension to format.
     */
    static Map<String,Class<? extends DataFormat>> extToFormat = 
        new HashMap<String, Class<? extends DataFormat>>();
    static {
        //extToFormat.put("shp", ShapefileFormat.class);
    }

    /**
     * looks up a format based on file extension.
     */
    public static DataFormat lookup(File file) {
        String ext = FilenameUtils.getExtension(file.getName());
        if (ext != null && extToFormat.containsKey(ext)) {
            Class<? extends DataFormat> clazz = extToFormat.get(ext);
            try {
                return clazz.newInstance();
            } 
            catch(Exception e) {}
        }

        //look for a datastore that can handle the file 
        FileDataStoreFactorySpi factory = FileDataStoreFinder.getDataStoreFactory(ext);
        if (factory != null) {
            return new DataStoreFormat(factory);
        }

        //look for a gridformat that can handle the file
        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        if (format != null && !(format instanceof UnknownFormat)) {
            return new GridFormat(format);
        }
        
        return null;
    }

    /**
     * Looks up a format based on a set of connection parameters. 
     */
    public static DataFormat lookup(Map<String,Serializable> params) {
        DataStoreFactorySpi factory = (DataStoreFactorySpi) DataStoreUtils.aquireFactory(params);
        if (factory != null) {
            return new DataStoreFormat(factory);
        }
        return null;
    }

    public abstract String getName();

    public abstract boolean canRead(ImportData data) throws IOException;

    public abstract StoreInfo createStore(ImportData data, WorkspaceInfo workspace, Catalog catalog) 
        throws IOException;

    public abstract List<ImportItem> list(ImportData data, Catalog catalog) throws IOException;
}
