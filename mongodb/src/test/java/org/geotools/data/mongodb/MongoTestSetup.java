package org.geotools.data.mongodb;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.mongodb.BasicDBList;
import com.mongodb.DB;

public abstract class MongoTestSetup {

    public abstract void setUp(DB db);

    protected abstract void setUpDataStore(MongoDataStore dataStore);

    public MongoDataStore createDataStore(Properties fixture) throws IOException {
        MongoDataStore dataStore = new MongoDataStoreFactory().createDataStore((Map)fixture);
        setUpDataStore(dataStore);
        return dataStore;
    }

    protected BasicDBList list(Object... values) {
        BasicDBList list = new BasicDBList();
        for (Object v : values) {
            list.add(v);
        }
        return list;
    }
}
