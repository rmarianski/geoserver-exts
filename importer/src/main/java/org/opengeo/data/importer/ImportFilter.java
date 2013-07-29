package org.opengeo.data.importer;

public interface ImportFilter {

    static ImportFilter ALL = new ImportFilter() {
        public boolean include(ImportTask task) {
            return true;
        }
    };

    boolean include(ImportTask task);
}
