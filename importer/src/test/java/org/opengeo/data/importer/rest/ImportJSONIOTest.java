package org.opengeo.data.importer.rest;

import java.io.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.rest.PageInfo;
import static org.junit.Assert.*;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.ImporterTestSupport;
import org.opengeo.data.importer.transform.DateFormatTransform;
import org.opengeo.data.importer.transform.TransformChain;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class ImportJSONIOTest extends ImporterTestSupport {
    private ImportJSONIO importio;
    private PageInfo info;
    private ByteArrayOutputStream buf;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        File dir = unpack("shape/archsites_epsg_prj.zip");
        importer.createContext(new Directory(dir));
        importio = new ImportJSONIO(importer);

        info = new PageInfo();
        info.setBasePath("basePath");
        info.setBaseURL("baseURL");
        info.setPagePath("pagePath");
        info.setRootPath("rootPath");

        newBuffer();
    }

    private void newBuffer() {
        buf = new ByteArrayOutputStream();
    }

    private JSONObject buffer() {
        return JSONObject.fromObject(new String(buf.toByteArray()));
    }

    private InputStream stream(JSONObject json) {
        return new ByteArrayInputStream(json.toString().getBytes());
    }

    public void testSettingTargetStore() throws IOException {
        ImportTask task = importer.getContext(0).getTasks().get(0);
        importio.task(task, info, buf);

        // update with new target
        JSONObject json = buffer();
        JSONObject target = new JSONObject();
        JSONObject dataStore = new JSONObject();
        JSONObject workspace = new JSONObject();
        dataStore.put("name", "foobar");
        workspace.put("name", getCatalog().getDefaultWorkspace().getName());
        dataStore.put("workspace", workspace);
        target.put("dataStore", dataStore);
        json.getJSONObject("task").put("target", target);

        ImportTask parsed = importio.task(stream(json));
        StoreInfo store = parsed.getStore();
        assertNotNull(store);
        assertEquals("foobar", store.getName());
        assertEquals(getCatalog().getDefaultWorkspace().getName(), store.getWorkspace().getName());
    }

    public void testAddingDateTransform() throws IOException {
        ImportItem task = importer.getContext(0).getTasks().get(0).getItems().get(0);
        importio.item(task, info, buf);
        
        // update with transform
        JSONObject json = buffer().getJSONObject("item");
        JSONObject tchain = json.getJSONObject("transformChain");
        JSONArray transforms = new JSONArray();
        JSONObject dateTransform = new JSONObject();
        dateTransform.put("type", "dateFormatTransform");
        dateTransform.put("field", "foobar");
        dateTransform.put("format", "yyyy-MM-dd");
        transforms.add(dateTransform);
        tchain.put("transforms", transforms);

        ImportItem item = importio.item(stream(json));
        assertNotNull(item);
        TransformChain chain = item.getTransform();
        assertNotNull(chain);
        assertEquals(1, chain.getTransforms().size());
        DateFormatTransform dft = (DateFormatTransform) chain.getTransforms().get(0);
        assertEquals("foobar",dft.getField());
        assertEquals("yyyy-MM-dd",dft.getDatePattern().dateFormat().toPattern());

    }
}
