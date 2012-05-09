package org.opengeo.data.importer.web;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.data.store.DataAccessEditPage;

public class DataStoreEditPage extends DataAccessEditPage {

    public DataStoreEditPage(DataStoreInfo store) {
        // TODO temporary hack
        super(store.getId());
    }
    // TODO temporary removal of doSaveStore(CoverageStoreInfo info) override
}
