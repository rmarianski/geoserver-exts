package org.opengeo.data.importer.transform;

import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportItem;

/**
 * Vector transform that is performed after an import has been completed.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public interface PostVectorTransform extends VectorTransform {

    void apply(ImportItem item, ImportData data) throws Exception;
}
