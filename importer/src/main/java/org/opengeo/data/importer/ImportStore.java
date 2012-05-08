package org.opengeo.data.importer;

import java.util.Iterator;

/**
 * Data access interface for persisting imports.
 * 
 * @todo refactor various queries into query object
 * @author Justin Deoliveira, OpenGeo
 */
public interface ImportStore {

    public interface ImportVisitor {
        void visit (ImportContext context);
    }

    void init();

    ImportContext get(long id);

    void add(ImportContext context);

    void save(ImportContext context);

    void remove(ImportContext importContext);

    void removeAll();

    Iterator<ImportContext> iterator();

    Iterator<ImportContext> allNonCompleteImports();
    
    Iterator<ImportContext> importsByUser(String user);

    void query(ImportVisitor visitor);

    void destroy();
}
