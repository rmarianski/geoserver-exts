package org.opengeo.data.importer.mosaic;

import java.io.File;

import org.junit.Test;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.ImporterTestSupport;

public class ImporterMosaicTest extends ImporterTestSupport {

    @Test
    public void testSimpleMosaic() throws Exception {
        File dir = unpack("mosaic/bm.zip");
        ImportContext context = importer.createContext(new Mosaic(dir));
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertTrue(task.getData() instanceof Mosaic);
        assertTrue(task.getData().getFormat() instanceof MosaicFormat);

        importer.run(context);

        runChecks(dir.getName());
    }
}
