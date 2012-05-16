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
        assertNotNull("Failure finding locations csv file", locationsResource);
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
    public void testCreateNewDataStore() throws IOException {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("file", file);
        FileDataStore dataStore = csvDataStoreFactory.createDataStore(map);
        assertNotNull("Failure creating data store", dataStore);
    }

    @Test
    public void testCreateDataStoreURL() throws MalformedURLException, IOException {
        FileDataStore dataStore = csvDataStoreFactory.createDataStore(locationsResource);
        assertNotNull("Failure creating data store", dataStore);
    }

    @Test
    public void testGetTypeName() throws IOException {
        URL resource = CSVDataStoreFactory.class.getResource("locations.csv");
        assertNotNull("Failure finding locations csv file", resource);
        FileDataStore dataStore = csvDataStoreFactory.createDataStore(resource);
        String[] typeNames = dataStore.getTypeNames();
        assertEquals("Invalid number of type names", 1, typeNames.length);
        assertEquals("Invalid type name", "locations", typeNames[0]);
    }

}
