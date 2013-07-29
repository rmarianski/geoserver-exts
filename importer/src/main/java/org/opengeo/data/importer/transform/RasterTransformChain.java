package org.opengeo.data.importer.transform;

import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportTask;

/**
 * @todo implement me
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class RasterTransformChain extends TransformChain<RasterTransform> {

    @Override
    public void pre(ImportTask task, ImportData data) throws Exception {
        if (transforms.size() > 0) {
            throw new RuntimeException("Not implemented");
        }
    }

    @Override
    public void post(ImportTask task, ImportData data) throws Exception {
    }
    
}
