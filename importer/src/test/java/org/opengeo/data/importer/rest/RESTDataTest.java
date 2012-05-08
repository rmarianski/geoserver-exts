package org.opengeo.data.importer.rest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.opengeo.data.importer.ImporterTestSupport;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Properties;
import javax.servlet.Filter;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.restlet.data.MediaType;

public class RESTDataTest extends ImporterTestSupport {

    public void testSingleFileUpload() throws Exception {
        int i = postNewImport();
        int t = postNewTaskAsMultiPartForm(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t);
        assertEquals("READY", task.getString("state"));

        postImport(i);
        runChecks("archsites");
    }

    public void testFilePut() throws Exception {
        int i = postNewImport();
        int t1 = putNewTask(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t1);
        assertEquals("READY", task.getString("state"));
        
        postImport(i);
        runChecks("archsites");
    }

    public void testMultipleFileUpload() throws Exception {
        int i = postNewImport();
        int t1 = postNewTaskAsMultiPartForm(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t1);
        assertEquals("READY", task.getString("state"));

        int t2 = postNewTaskAsMultiPartForm(i, "shape/bugsites_esri_prj.tar.gz");
        task = getTask(i, t2);
        assertEquals("READY", task.getString("state"));
        
        postImport(i);
        runChecks("archsites");
        runChecks("bugsites");
    }

    public void testFileUploadWithConfigChange() throws Exception {
        int i = postNewImport();
        int t = postNewTaskAsMultiPartForm(i, "shape/archsites_no_crs.zip");

        JSONObject task = getTask(i, t);
        assertEquals("INCOMPLETE", task.getString("state"));
        
        JSONObject item = getItem(i, t, 0);
        assertEquals("NO_CRS", item.getString("state"));

        String json = 
        "{" +
          "\"item\": {" +
            "\"resource\": {" +
                "\"featureType\": {" +
                    "\"srs\": \"EPSG:4326\"" + 
                 "}" +
             "}" +
           "}" + 
        "}";
        putItem(i, t, 0, json);

        item = getItem(i, t, 0);
        assertEquals("READY", item.getString("state"));
        assertEquals("gs_archsites", item.getJSONObject("layer").getJSONObject("layer")
                .getJSONObject("defaultStyle").getString("name"));
        json = 
        "{" +
          "\"item\": {" +
            "\"layer\": {" +
              "\"layer\": {" +
                "\"defaultStyle\": {" +
                    "\"name\": \"point\"" + 
                 "}" +
               "}" +
             "}" +
           "}" + 
        "}";
        putItem(i, t, 0, json);

        item = getItem(i, t, 0);
        
        assertEquals("READY", item.getString("state"));
        assertEquals("point", item.getJSONObject("layer").getJSONObject("layer")
            .getJSONObject("defaultStyle").getString("name"));

        postImport(i);
        runChecks("archsites");
    }

    public void testSingleFileUploadIntoDb() throws Exception {
        DataStoreInfo acme = createH2DataStore("sf", "acme");

        String json = 
            "{" + 
                "\"import\": { " + 
                    "\"targetWorkspace\": {" +
                       "\"workspace\": {" + 
                           "\"name\": \"sf\"" + 
                       "}" + 
                    "}," +
                    "\"targetStore\": {" +
                        "\"dataStore\": {" + 
                            "\"name\": \"acme\"" + 
                        "}" + 
                     "}" +
                "}" + 
            "}";
        int i = postNewImport(json);
        int t = postNewTaskAsMultiPartForm(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t);
        assertEquals("READY", task.getString("state"));

        Catalog cat = importer.getCatalog();
        assertTrue(cat.getFeatureTypesByDataStore(acme).isEmpty());

        postImport(i);

        assertEquals(1, cat.getFeatureTypesByDataStore(acme).size());
        assertNotNull(cat.getFeatureTypeByStore(acme, "archsites"));
        runChecks("sf:archsites");
    }

    public void testSingleFileUploadIntoDb2() throws Exception {
        DataStoreInfo acme = createH2DataStore("sf", "acme");

        String json = 
            "{" + 
                "\"import\": { " + 
                    "\"targetStore\": {" +
                        "\"dataStore\": {" + 
                            "\"name\": \"acme\", " +
                            "\"workspace\": {" + 
                                "\"name\": \"sf\"" + 
                            "}" + 
                        "}" + 
                     "}" +
                "}" + 
            "}";
        int i = postNewImport(json);
        int t = postNewTaskAsMultiPartForm(i, "shape/archsites_epsg_prj.zip");

        JSONObject task = getTask(i, t);
        assertEquals("READY", task.getString("state"));

        Catalog cat = importer.getCatalog();
        assertTrue(cat.getFeatureTypesByDataStore(acme).isEmpty());

        postImport(i);

        assertEquals(1, cat.getFeatureTypesByDataStore(acme).size());
        assertNotNull(cat.getFeatureTypeByStore(acme, "archsites"));
        runChecks("sf:archsites");
    }

    JSONObject getTask(int imp, int task) throws Exception {
        JSON json = getAsJSON(String.format("/rest/imports/%d/tasks/%d", imp, task));
        return ((JSONObject)json).getJSONObject("task");
    }

    JSONObject getItem(int imp, int task, int item) throws Exception {
        JSON json = getAsJSON(String.format("/rest/imports/%d/tasks/%d/items/%d", imp, task, item));
        return ((JSONObject)json).getJSONObject("item");
    }

    void putItem(int imp, int task, int item, String json) throws Exception {
        MockHttpServletResponse resp = putAsServletResponse(
            String.format("/rest/imports/%d/tasks/%d/items/%d", imp, task, item), json, "application/json");
        assertEquals(202, resp.getStatusCode());
    }

    int postNewTaskAsMultiPartForm(int imp, String data) throws Exception {
        File dir = unpack(data);

        List<Part> parts = new ArrayList<Part>(); 
        for (File f : dir.listFiles()) {
            parts.add(new FilePart(f.getName(), f));
        }
        MultipartRequestEntity multipart = new MultipartRequestEntity(
            parts.toArray(new Part[parts.size()]), new PostMethod().getParams());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        multipart.writeRequest(bout);

        MockHttpServletRequest req = createRequest("/rest/imports/" + imp + "/tasks");
        req.setContentType(multipart.getContentType());
        req.addHeader("Content-Type", multipart.getContentType());
        req.setMethod("POST");
        req.setBodyContent(bout.toByteArray());

        MockHttpServletResponse resp = dispatch(req);
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );

        assertTrue(resp.getHeader("Location").matches(".*/imports/"+imp+"/tasks/\\d"));
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);

        JSONObject task = json.getJSONObject("task");
        return task.getInt("id");
    }

    int putNewTask(int imp, String data) throws Exception {
        File zip = getTestDataFile(data);
        byte[] payload = new byte[ (int) zip.length()];
        FileInputStream fis = new FileInputStream(zip);
        fis.read(payload);
        fis.close();

        MockHttpServletRequest req = createRequest("/rest/imports/" + imp + "/tasks/" + new File(data).getName());
        req.setHeader("Content-Type", MediaType.APPLICATION_ZIP.toString());
        req.setMethod("PUT");
        req.setBodyContent(payload);

        MockHttpServletResponse resp = dispatch(req);
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );

        assertTrue(resp.getHeader("Location").matches(".*/imports/"+imp+"/tasks/\\d"));
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);

        JSONObject task = json.getJSONObject("task");
        return task.getInt("id");
    }

    int postNewImport() throws Exception {
        return postNewImport(null);
    }
    int postNewImport(String body) throws Exception {
        MockHttpServletResponse resp = body == null ? postAsServletResponse("/rest/imports", "")
            : postAsServletResponse("/rest/imports", body, "application/json");
        
        assertEquals(201, resp.getStatusCode());
        assertNotNull( resp.getHeader( "Location") );
        assertTrue(resp.getHeader("Location").matches(".*/imports/\\d"));
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);
        JSONObject imprt = json.getJSONObject("import");
        return imprt.getInt("id");
    }

    void postImport(int imp) throws Exception {
        MockHttpServletResponse resp = postAsServletResponse("/rest/imports/" + imp, "");
        assertEquals(204, resp.getStatusCode());
    }
}
