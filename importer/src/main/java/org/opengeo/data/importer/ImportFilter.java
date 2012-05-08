package org.opengeo.data.importer;

public interface ImportFilter {

    static ImportFilter ALL = new ImportFilter() {
        public boolean include(ImportTask task) {
            return true;
        }

        public boolean include(ImportItem item) {
            return true;
        }
    };

    boolean include(ImportTask task);

    boolean include(ImportItem item);
}
