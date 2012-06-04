package org.opengeo.data.importer.bdb;

import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportStore.ImportVisitor;

class CountingVisitor implements ImportVisitor {

    int count = 0;

    public void visit(ImportContext context) {
        count++;
    }

    public int getCount() {
        return count;
    }
}