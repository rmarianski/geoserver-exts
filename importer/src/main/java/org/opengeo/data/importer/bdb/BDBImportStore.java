package org.opengeo.data.importer.bdb;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.geoserver.catalog.LayerInfo;
import org.opengeo.data.importer.DataStoreFormat;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.ImportStore;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.Importer;
import org.opengeo.data.importer.SpatialFile;
import org.opengeo.data.importer.Table;
import org.opengeo.data.importer.transform.VectorTransformChain;

import com.sleepycat.bind.EntityBinding;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Sequence;
import com.sleepycat.je.SequenceConfig;
import java.util.logging.Logger;
import com.sleepycat.je.Transaction;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geotools.util.logging.Logging;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;

/**
 * Import store implementation based on Berkley DB Java Edition.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class BDBImportStore implements ImportStore {
    
    static Logger LOGGER = Logging.getLogger(Importer.class);

    Importer importer;

    Database db;
    Database seqDb;

    Sequence importIdSeq;
    EntryBinding<ImportContext> importBinding;

    public BDBImportStore(Importer importer) {
        this.importer = importer;
    }

    public void init() {
        //create the db environment
        EnvironmentConfig envCfg = new EnvironmentConfig();
        envCfg.setAllowCreate(true);
        envCfg.setCacheMode(CacheMode.DEFAULT);
        envCfg.setLockTimeout(1000, TimeUnit.MILLISECONDS);
        envCfg.setDurability(Durability.COMMIT_WRITE_NO_SYNC);
        envCfg.setSharedCache(true);
        envCfg.setTransactional(true);
        envCfg.setConfigParam("je.log.fileMax", String.valueOf(100 * 1024 * 1024));

        File dbRoot = new File(importer.getImportRoot(), "bdb");
        dbRoot.mkdir();
        
        Environment env = new Environment(dbRoot, envCfg);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);

        db = env.openDatabase(null, "imports", dbConfig);
         
        SequenceConfig seqConfig = new SequenceConfig();
        seqConfig.setAllowCreate(true);
        seqDb = env.openDatabase(null, "seq", dbConfig);
        importIdSeq = 
            seqDb.openSequence(null, new DatabaseEntry("import_id".getBytes()), seqConfig);
        
        importBinding = new XStreamInfoSerialBinding<ImportContext>(
            importer.createXStreamPersister(), ImportContext.class);
    }


    public ImportContext get(long id) {
        DatabaseEntry val = new DatabaseEntry();
        OperationStatus op = db.get(null, key(id), val, LockMode.DEFAULT);
        if (op == OperationStatus.NOTFOUND) {
            return null;
        }

        ImportContext context = importBinding.entryToObject(val);
        return reattach(context);
    }

    ImportContext reattach(ImportContext context) {
        //reload store and workspace objects from catalog so they are "attached" with 
        // the proper references to the catalog initialized
        context.reattach();
        Catalog catalog = importer.getCatalog();
        for (ImportTask task : context.getTasks()) {
            StoreInfo store = task.getStore();
            if (store != null && store.getId() != null) {
                task.setStore(catalog.getStore(store.getId(), StoreInfo.class));
                //((StoreInfoImpl) task.getStore()).setCatalog(catalog); // @todo remove if the above sets catalog
            }
            for (ImportItem item : task.getItems()) {
                if (item.getLayer() != null) {
                    LayerInfo l = item.getLayer();
                    if (l.getDefaultStyle() != null && l.getDefaultStyle().getId() != null) {
                        l.setDefaultStyle(catalog.getStyle(l.getDefaultStyle().getId()));
                    }
                    if (l.getResource() != null) {
                        ResourceInfo r = l.getResource();
                        r.setCatalog(catalog);

                        if (r.getStore() == null) {
                            r.setStore(store);
                        }

                        if (r.getStore().getCatalog() == null) {
                            ((StoreInfoImpl) r.getStore()).setCatalog(catalog);
                        }

                    }
                }
            }
        }
        return context;
    }

    ImportContext dettach(ImportContext context) {
        Catalog catalog = importer.getCatalog();
        for (ImportTask task : context.getTasks()) {
            StoreInfo store = task.getStore();
            if (store != null && store.getId() != null) {
                task.setStore(catalog.detach(store));
            }
        }
        return context;
    }

    public void add(ImportContext context) {
        context.setId(importIdSeq.get(null, 1));

        put(context);
    }

    public void remove(ImportContext importContext) {
        db.delete(null, key(importContext) );
    }

    public void removeAll() {

        Transaction tx = db.getEnvironment().beginTransaction(null, null);
        Cursor c  = db.openCursor(tx,null);

        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry val = new DatabaseEntry();

        LongBinding keyBinding = new LongBinding();
        List<Long> ids = new ArrayList();

        OperationStatus op = null;
        while((op  = c.getNext(key, val, LockMode.DEFAULT)) == OperationStatus.SUCCESS) {
            ids.add(LongBinding.entryToLong(key));
        }
        c.close();

        for (Long id : ids) {
            keyBinding.objectToEntry(id, key);
            db.delete(tx, key);
        }

        tx.commit();
    }
   
    public void save(ImportContext context) {
        dettach(context);
        if (context.getId() == null) {
            add(context);
        }
        else {
            put(context);
        }
    }

    public Iterator<ImportContext> iterator() {
        return new StoredMap<Long, ImportContext>(db, new LongBinding(), importBinding, false)
            .values().iterator();
    }

    public Iterator<ImportContext> allNonCompleteImports() {
        // if this becomes too slow a secondary database could be used for indexing
        return new FilterIterator(iterator(), new Predicate() {

            public boolean evaluate(Object o) {
                return ((ImportContext) o).getState() != ImportContext.State.COMPLETE;
            }
        });
    }
    
    public Iterator<ImportContext> importsByUser(final String user) {        
        // if this becomes too slow a secondary database could be used for indexing
        return new FilterIterator(allNonCompleteImports(), new Predicate() {

            public boolean evaluate(Object o) {
                return user.equals( ((ImportContext) o).getUser() );
            }
        });
    }

    public void query(ImportVisitor visitor) {
        Cursor c  = db.openCursor(null, null);
        try {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry val = new DatabaseEntry();
    
            OperationStatus op = null;
            while((op  = c.getNext(key, val, LockMode.DEFAULT)) == OperationStatus.SUCCESS) {
                visitor.visit(importBinding.entryToObject(val));
            }
        }
        finally {
            c.close();
        }
    }

    void put(ImportContext context) {
        DatabaseEntry val = new DatabaseEntry();
        importBinding.objectToEntry(context, val);

        db.put(null, key(context), val);
    }

    DatabaseEntry key(ImportContext context) {
        return key(context.getId());
    }

    DatabaseEntry key(long id) {
        DatabaseEntry key = new DatabaseEntry();
        new LongBinding().objectToEntry(id, key);
        return key;
    }

    byte[] toBytes(long l) {
        byte[] b = new byte[8];
        b[0]   = (byte)(0xff & (l >> 56));
        b[1] = (byte)(0xff & (l >> 48));
        b[2] = (byte)(0xff & (l >> 40));
        b[3] = (byte)(0xff & (l >> 32));
        b[4] = (byte)(0xff & (l >> 24));
        b[5] = (byte)(0xff & (l >> 16));
        b[6] = (byte)(0xff & (l >> 8));
        b[7] = (byte)(0xff & l);
        return b;
    }
    public void destroy() {
        //destroy the db environment
        Environment env = db.getEnvironment();
        seqDb.close();
        db.close();
        env.close();
    }
}
