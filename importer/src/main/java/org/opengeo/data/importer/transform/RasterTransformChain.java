package org.opengeo.data.importer.transform;

import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportItem;

/**
 * @todo implement me
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class RasterTransformChain extends TransformChain<RasterTransform> {

    @Override
    public void pre(ImportItem item, ImportData data) throws Exception {
        if (transforms.size() > 0) {
            throw new RuntimeException("Not implemented");
        }
    }

    @Override
    public void post(ImportItem item, ImportData data) throws Exception {
    }
    
}
