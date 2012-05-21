package org.opengeo.data.csv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.FileDataStore;
import org.junit.Before;
import org.junit.Test;

public class CSVDataStoreFactoryTest {

    private CSVDataStoreFactory csvDataStoreFactory;

    private File file;

    private URL locationsResource;

    @Before
    public void setUp() {
        csvDataStoreFactory = new CSVDataStoreFactory();
        locationsResource = CSVDataStoreFactory.class.getResource("locations.csv");
        assert locationsResource != null : "Could not find locations.csv resource";
        file = new File(locationsResource.getFile());
    }

    @Test
    public void testBasicGetters() throws MalformedURLException {
        assertEquals("CSV", csvDataStoreFactory.getDisplayName());
        assertEquals("Comma delimited text file", csvDataStoreFactory.getDescription());
        assertTrue(csvDataStoreFactory.canProcess(locationsResource));
        assertTrue(csvDataStoreFactory.getImplementationHints().isEmpty());
        assertArrayEquals(new String[] { ".csv" }, csvDataStoreFactory.getFileExtensions());
        assertNotNull("Invalid Parameter Info", csvDataStoreFactory.getParametersInfo());
    }

    @Test
    public void testCreateDataStoreFileParams() throws Exception {
        Map<String, Serializable> fileParams = new HashMap<String, Serializable>(1);
        fileParams.put("file", file);
        FileDataStore dataStore = csvDataStoreFactory.createDataStore(fileParams);
        assertNotNull("Could not create datastore from file params", dataStore);
    }

    @Test
    public void testCreateDataStoreURLParams() throws Exception {
        Map<String, Serializable> urlParams = new HashMap<String, Serializable>(1);
        urlParams.put("url", locationsResource);
        FileDataStore dataStore = csvDataStoreFactory.createDataStore(urlParams);
        assertNotNull("Could not create datastore from url params", dataStore);
    }

    @Test
    public void testCreateDataStoreURL() throws MalformedURLException, IOException {
        FileDataStore dataStore = csvDataStoreFactory.createDataStore(locationsResource);
        assertNotNull("Failure creating data store", dataStore);
    }

    @Test
    public void testGetTypeName() throws IOException {
        FileDataStore dataStore = csvDataStoreFactory.createDataStore(locationsResource);
        String[] typeNames = dataStore.getTypeNames();
        assertEquals("Invalid number of type names", 1, typeNames.length);
        assertEquals("Invalid type name", "locations", typeNames[0]);
    }

    @Test
    public void testCanProcessFileParams() {
        Map<String, Serializable> fileParams = new HashMap<String, Serializable>(1);
        fileParams.put("file", file);
        assertTrue("Did not process file params", csvDataStoreFactory.canProcess(fileParams));
    }

    @Test
    public void testCanProcessURLParams() {
        Map<String, Serializable> urlParams = new HashMap<String, Serializable>(1);
        urlParams.put("url", locationsResource);
        assertTrue("Did not process url params", csvDataStoreFactory.canProcess(urlParams));
    }

    @Test
    public void testInvalidParamsCreation() throws Exception {
        Map<String, Serializable> params = new HashMap<String, Serializable>(0);
        try {
            csvDataStoreFactory.createDataStore(params);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
            return;
        } catch (Exception e) {
        }
        assertTrue("Did not throw illegal argument exception for null file", false);
    }

    @Test
    public void testFileDoesNotExist() throws Exception {
        try {
            csvDataStoreFactory.createDataStoreFromFile(new File("/tmp/does-not-exist.csv"));
        } catch (IllegalArgumentException e) {
            assertTrue(true);
            return;
        } catch (Exception e) {
        }
        assertTrue("Did not throw illegal argument exception for non-existent file", false);
    }
}
