package org.opengeo.data.importer;

import java.io.IOException;

import org.geotools.data.FeatureReader;


/**
 * Base class for vector based formats.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class VectorFormat extends DataFormat {

    /**
     * Reads features from the data for the specified import item. 
     */
    public abstract FeatureReader read(ImportData data, ImportItem item) throws IOException;

    /**
     * Disposes the reader for the specified import item.  
     */
    public abstract void dispose(FeatureReader reader, ImportItem item) throws IOException;
    
    /**
     * Get the number of features from the data for the specified import item.
     */
    public abstract int getFeatureCount(ImportData data, ImportItem item) throws IOException;

}
