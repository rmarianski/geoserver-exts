package org.geotools.data.mongodb;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;

public class MongoDataStoreFactory extends AbstractDataStoreFactory {

    public static final Param DATASTORE_URI = new Param("data_store", String.class, "MongoDB URI", true, "mongodb://localhost/<database name>");
    public static final Param SCHEMASTORE_URI = new Param("schema_store", String.class, "Schema Store URI", true, "file://<absolute path>");
    
    @Override
    public String getDisplayName() {
        return "MongoDB";
    }
    
    @Override
    public String getDescription() {
        return "MongoDB database";
    }
    
    @Override
    public Param[] getParametersInfo() {
        return new Param[]{DATASTORE_URI, SCHEMASTORE_URI};
    }

    @Override
    public MongoDataStore createDataStore(Map<String, Serializable> params) throws IOException {
        return new MongoDataStore(
                (String)DATASTORE_URI.lookUp(params), (String)SCHEMASTORE_URI.lookUp(params));
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        throw new UnsupportedOperationException();
    }

}
