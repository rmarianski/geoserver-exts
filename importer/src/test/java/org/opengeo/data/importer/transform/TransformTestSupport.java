package org.opengeo.data.importer.transform;

import static org.easymock.classextension.EasyMock.*;

import java.beans.PropertyDescriptor;
import java.io.StringWriter;
import junit.framework.TestCase;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;

import org.geoserver.rest.PageInfo;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.Importer;
import org.opengeo.data.importer.rest.ImportJSONWriter;
import org.opengeo.data.importer.rest.ImportJSONReader;
import org.springframework.beans.BeanUtils;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public abstract class TransformTestSupport extends TestCase {

    public void doJSONTest(ImportTransform transform) throws Exception {
        StringWriter buffer = new StringWriter();

        Importer im = createNiceMock(Importer.class);
        PageInfo pi = createNiceMock(PageInfo.class);
        
        replay(im, pi);

        ImportJSONWriter jsonio = new ImportJSONWriter(im, pi, buffer);

        ImportContext c = new ImportContext(0);
        c.addTask(new ImportTask());

        jsonio.transform(transform, 0, c.task(0), true, 1);

        ImportTransform transform2 = new ImportJSONReader(im, buffer.toString()).transform();
        PropertyDescriptor[] pd = BeanUtils.getPropertyDescriptors(transform.getClass());

        for (int i = 0; i < pd.length; i++) {
            assertEquals("expected same value of " + pd[i].getName(),
                    pd[i].getReadMethod().invoke(transform),
                    pd[i].getReadMethod().invoke(transform2));
        }
    }
}
