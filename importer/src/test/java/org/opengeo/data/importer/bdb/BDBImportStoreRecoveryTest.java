package org.opengeo.data.importer.bdb;

import java.io.File;

import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImporterTestSupport;

public class BDBImportStoreRecoveryTest extends ImporterTestSupport {

    public void testUpgradeFromXStream() throws Exception {
        BDBImportStore store = new BDBImportStore(importer);
        store.setBinding(BDBImportStore.BindingType.XSTREAM);
        store.init();

        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(new Directory(dir));
        store.add(context);
        store.destroy();

        store = new BDBImportStore(importer);
        store.setBinding(BDBImportStore.BindingType.SERIAL);
        store.init();

        CountingVisitor v = new CountingVisitor();
        store.query(v);
        assertEquals(0, v.getCount());

        long id = context.getId();
        assertNull(store.get(id));
        store.destroy();
    }
}
