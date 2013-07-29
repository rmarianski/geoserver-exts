package org.opengeo.data.importer.transform;

import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportTask;

/**
 * Vector transform that is performed before input occurs.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public interface PreVectorTransform extends VectorTransform {

    void apply(ImportTask task, ImportData data) throws Exception;
}
