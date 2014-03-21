/*
 * 
 */

package org.geotools.data.mongodb;

import java.io.IOException;
import java.util.List;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public interface MongoSchemaStore {
    
    void storeSchema(SimpleFeatureType schema) throws IOException;
    
    SimpleFeatureType retrieveSchema(Name name) throws IOException;
    
    void deleteSchema(Name name) throws IOException;
    
    List<String> typeNames();
    
    void close();
}
