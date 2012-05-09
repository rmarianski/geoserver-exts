package org.opengeo.data.importer.web;

import org.geoserver.catalog.CoverageStoreInfo;

public class CoverageStoreEditPage extends
        org.geoserver.web.data.store.CoverageStoreEditPage {

    public CoverageStoreEditPage(CoverageStoreInfo store) {
        // TODO temporary hack
        super(store.getId());
    }
    // TODO temporary removal of doSaveStore(CoverageStoreInfo info) override

}
