package org.geoserver.uploader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ResourceUploaderTest extends GeoServerTestSupport {

    Catalog catalog;

    GeoServerDataDirectory dataDir;

    UploaderConfigPersister configPersister;

    UploadLifeCyleManager lifeCycleManager;

    ResourceUploaderResource uploader;

    final String archsitesTestResource = "shapes/archsites_epsg_prj.zip";

    final String bugsitesTestResource = "shapes/bugsites_esri_prj.tar.gz";

    final String coverageTestResource = "geotiffs/EmissiveCampania.tif.bz2";

    final String archsitesFileName = "archsites.zip";

    final String bugsitesFileName = "bugsites.tar.gz";

    final String coverageTestName = "EmissiveCampania.tif.bz2";

    FileItem archSitesFileItem;

    FileItem bugSitesFileItem;

    Map<String, Object> params;

    @Override
    protected boolean useLegacyDataDirectory() {
        return false;
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        /*
         * deactivate the arcsde raster logger so it doesn't complain about the esri jars not being
         * on the classpath
         */
        Logging.getLogger("org.geotools.arcsde.ArcSDERasterFormatFactory").setLevel(Level.OFF);
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWcs11Coverages();
    }

    @Override
    public void setUpInternal() {
        catalog = getCatalog();
        dataDir = getDataDirectory();
        lifeCycleManager = new UploadLifeCyleManager(dataDir);
        configPersister = new UploaderConfigPersister(catalog, dataDir);
        uploader = new ResourceUploaderResource(catalog, lifeCycleManager, configPersister);
        try {
            archSitesFileItem = fileItemMock(archsitesFileName, archsitesTestResource);
            bugSitesFileItem = fileItemMock(bugsitesFileName, bugsitesTestResource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        params = new HashMap<String, Object>();

    }

    public void testNoFileProvided() throws Exception {
        try {
            uploader.uploadLayers(params);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("file"));
        }
    }

    public void testWrongWorkspaceParam() throws Exception {
        params.put("file", archSitesFileItem);
        params.put("workspace", "nonExistentWorkspace");
        try {
            uploader.uploadLayers(params);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("workspace"));
        }
    }

    public void testInexistentStoreParam() throws Exception {
        params.put("file", archSitesFileItem);
        params.put("workspace", catalog.getDefaultWorkspace().getName());
        params.put("store", "nonExistentStore");
        try {
            uploader.uploadLayers(params);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("store"));
        }
    }

    public void testFailIfRequestedStoreIsCoverageStore() throws Exception {

        List<CoverageStoreInfo> coverageStores = catalog.getCoverageStores();
        assertTrue(coverageStores.size() > 0);
        CoverageStoreInfo coverageStoreInfo = coverageStores.get(0);
        String workspace = coverageStoreInfo.getWorkspace().getName();
        String coverageStoreName = coverageStoreInfo.getName();

        params.put("file", archSitesFileItem);
        params.put("workspace", workspace);
        params.put("store", coverageStoreName);
        try {
            uploader.uploadLayers(params);
            fail("Expected IAE");
        } catch (InvalidParameterException e) {
            assertEquals("store", e.getLocator());
        }
    }

    /**
     * A type comming with a .prj file that doesn't match an EPSG code should be run through
     * {@link CRS#lookupEpsgCode CRS#lookupEpsgCode(crsFromPrjFile, extensive == true)}
     */
    // commenting out this test as I couldn't figure out why CRS.lookupEpsgCode(crs, true) doesn't
    // work in the test environment but it does work with the running application, even though I've
    // added gt-epsg-hsql as a test dependency
    public void _testLookUpSRS() throws Exception {
        params.put("file", bugSitesFileItem);// this one contains a .prj file in esri format
        List<LayerInfo> uploadLayers = uploader.uploadLayers(params);
        assertTrue(uploadLayers.size() == 1);
        LayerInfo layerInfo = uploadLayers.get(0);
        ResourceInfo resource = layerInfo.getResource();
        assertNotNull(resource);
        CoordinateReferenceSystem nativeCRS = resource.getNativeCRS();
        assertNotNull(nativeCRS);

        CoordinateReferenceSystem expected = CRS.decode("EPSG:26713");
        assertEquals(expected, nativeCRS);
    }

    public void testNoCRSProvided() throws Exception {
        FileItem fileItemMock = fileItemMock("archsites_no_crs.zip", "shapes/archsites_no_crs.zip");
        params.put("file", fileItemMock);// this one has no .prj file
        try {
            uploader.uploadLayers(params);
            fail("Expected IPE");
        } catch (MissingInformationException e) {
            assertEquals("crs", e.getLocator());
            String token = e.getToken();
            assertNotNull(token);
            File pendingUploadDir = lifeCycleManager.getPendingUploadDir(token);
            assertTrue(pendingUploadDir.exists());
        }
    }

    public void testUploadShpGeoServerDefaultWorkspace() throws Exception {
        configPersister.setDefaults(null, null);

        params.put("file", archSitesFileItem);

        List<LayerInfo> layers = uploader.uploadLayers(params);
        assertNotNull(layers);
        assertEquals(1, layers.size());
        LayerInfo layerInfo = layers.get(0);
        assertNotNull(layerInfo);
        assertEquals("archsites", layerInfo.getName());
        assertNotNull(catalog.getLayerByName(layerInfo.getName()));

        final File uploaded = dataDir.findDataFile("incoming", "archsites", "archsites.shp");
        assertNotNull(uploaded);

        ResourceInfo resource = layerInfo.getResource();
        assertNotNull(resource);

        StoreInfo store = resource.getStore();
        String expectedType = new ShapefileDataStoreFactory().getDisplayName();
        assertEquals(expectedType, store.getType());
        Map<String, Serializable> connParams = store.getConnectionParameters();
        URL fileURL = (URL) connParams.get(ShapefileDataStoreFactory.URLP.key);
        assertNotNull(fileURL);
        File importedFile = new File(fileURL.toURI());
        assertEquals(uploaded.getAbsoluteFile(), importedFile.getAbsoluteFile());
        WorkspaceInfo workspace = store.getWorkspace();
        assertEquals(catalog.getDefaultWorkspace(), workspace);
    }

    public void testUploadShpUploaderDefaultWorkspace() throws Exception {

        final WorkspaceInfo targetWs = getNonDefaultWorkspace();
        configPersister.setDefaults(targetWs, null);

        final String ws = targetWs.getName();
        params.put("file", archSitesFileItem);

        List<LayerInfo> layers = uploader.uploadLayers(params);
        LayerInfo layerInfo = layers.get(0);

        assertEquals("archsites", layerInfo.getName());
        assertEquals(ws, layerInfo.getResource().getStore().getWorkspace().getName());
    }

    public void testUploadShpUserSpecifiedWorkspace() throws Exception {
        final WorkspaceInfo targetWs = getNonDefaultWorkspace();
        final String ws = targetWs.getName();

        params.put("file", archSitesFileItem);
        params.put("workspace", ws);

        List<LayerInfo> layers = uploader.uploadLayers(params);
        LayerInfo layerInfo = layers.get(0);

        assertEquals("archsites", layerInfo.getName());
        assertEquals(ws, layerInfo.getResource().getStore().getWorkspace().getName());
    }

    public void testUploadShpUploadersDefaultDataStore() throws Exception {
        final WorkspaceInfo targetWs = getNonDefaultWorkspace();
        final DataStoreInfo targetDs;
        // create a datastore capable of creating new ft's
        targetDs = createDirectoryDataStore(targetWs);
        configPersister.setDefaults(targetWs, targetDs);

        params.put("file", archSitesFileItem);

        List<LayerInfo> layers = uploader.uploadLayers(params);
        LayerInfo layerInfo = layers.get(0);

        assertEquals("archsites", layerInfo.getName());
        StoreInfo store = layerInfo.getResource().getStore();
        assertEquals(targetDs, store);
    }

    public void testUploadShpUserSpecifiedDataStore() throws Exception {
        final WorkspaceInfo targetWs = getNonDefaultWorkspace();
        final DataStoreInfo targetDs;
        // create a datastore capable of creating new ft's
        targetDs = createDirectoryDataStore(targetWs);
        configPersister.setDefaults(null, null);

        params.put("file", archSitesFileItem);
        params.put("workspace", targetWs.getName());
        params.put("store", targetDs.getName());

        List<LayerInfo> layers = uploader.uploadLayers(params);
        LayerInfo layerInfo = layers.get(0);

        assertEquals("archsites", layerInfo.getName());
        StoreInfo store = layerInfo.getResource().getStore();
        assertEquals(targetDs, store);
    }

    public void testCantUploadToExistingSingleFileDataStore() throws Exception {

        final DataStoreInfo defaultDataStore = getCatalog().getDataStores().get(0);
        {
            assertNotNull(defaultDataStore);
            ResourcePool resourcePool = getCatalog().getResourcePool();
            DataAccessFactory dataStoreFactory = resourcePool.getDataStoreFactory(defaultDataStore);
            assertTrue(dataStoreFactory instanceof PropertyDataStoreFactory);
        }

        params.put("file", archSitesFileItem);
        params.put("workspace", defaultDataStore.getWorkspace().getName());
        params.put("store", defaultDataStore.getName());

        try {
            uploader.uploadLayers(params);
            fail("Expected IAE");
        } catch (IllegalArgumentException expected) {
            String message = expected.getMessage();
            assertTrue(message.toLowerCase().contains("invalid"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Expected IAE, but got " + e.getClass().getName());
        }
    }

    public void testUploadShpMultipleTimesAssignsNewNames() throws IOException,
            InvalidParameterException, MissingInformationException {

        archSitesFileItem = fileItemMock(archsitesFileName, archsitesTestResource);
        params.put("file", archSitesFileItem);
        LayerInfo layerInfo1 = uploader.uploadLayers(params).get(0);

        archSitesFileItem = fileItemMock(archsitesFileName, archsitesTestResource);
        params.put("file", archSitesFileItem);
        LayerInfo layerInfo2 = uploader.uploadLayers(params).get(0);

        archSitesFileItem = fileItemMock(archsitesFileName, archsitesTestResource);
        params.put("file", archSitesFileItem);
        LayerInfo layerInfo3 = uploader.uploadLayers(params).get(0);

        assertEquals("archsites", layerInfo1.getName());
        assertEquals("archsites_1", layerInfo2.getName());
        assertEquals("archsites_2", layerInfo3.getName());
    }

    public void testUploadCoverageGeoServerDefaultWorkspace() throws Exception {
        configPersister.setDefaults(null, null);
        FileItem coverageFileItem = fileItemMock(coverageTestName, coverageTestResource);
        params.put("file", coverageFileItem);
        LayerInfo layerInfo = uploader.uploadLayers(params).get(0);
        assertEquals("EmissiveCampania", layerInfo.getName());
        ResourceInfo resource = layerInfo.getResource();
        StoreInfo store = resource.getStore();
        assertTrue(store instanceof CoverageStoreInfo);
        assertEquals(getCatalog().getDefaultWorkspace(), store.getWorkspace());
    }

    public void testUploadCoverageUploaderDefaultWorkspace() throws Exception {
        WorkspaceInfo uploadersWs = getNonDefaultWorkspace();
        configPersister.setDefaults(uploadersWs, null);

        FileItem coverageFileItem = fileItemMock(coverageTestName, coverageTestResource);
        params.put("file", coverageFileItem);
        LayerInfo layerInfo = uploader.uploadLayers(params).get(0);
        assertEquals("EmissiveCampania", layerInfo.getName());
        ResourceInfo resource = layerInfo.getResource();
        StoreInfo store = resource.getStore();
        assertEquals(uploadersWs, store.getWorkspace());
    }

    public void testUploadCoverageUserSpecifiedWorkspace() throws Exception {
        configPersister.setDefaults(getCatalog().getDefaultWorkspace(), null);

        final WorkspaceInfo requestedWs = getNonDefaultWorkspace();
        FileItem coverageFileItem = fileItemMock(coverageTestName, coverageTestResource);
        params.put("file", coverageFileItem);
        params.put("workspace", requestedWs.getName());

        LayerInfo layerInfo = uploader.uploadLayers(params).get(0);
        assertEquals("EmissiveCampania", layerInfo.getName());
        ResourceInfo resource = layerInfo.getResource();
        StoreInfo store = resource.getStore();
        assertEquals(requestedWs, store.getWorkspace());
    }

    public void testUploadCoverageDataStoreParamIsIgnored() throws Exception {
        final WorkspaceInfo targetWs = getNonDefaultWorkspace();
        final DataStoreInfo datastore;
        // create a datastore capable of creating new ft's
        datastore = createDirectoryDataStore(targetWs);

        configPersister.setDefaults(getCatalog().getDefaultWorkspace(), null);

        FileItem coverageFileItem = fileItemMock(coverageTestName, coverageTestResource);
        params.put("file", coverageFileItem);
        params.put("workspace", targetWs.getName());
        params.put("store", datastore.getName());// it shouldn't matter

        LayerInfo layerInfo = uploader.uploadLayers(params).get(0);
        assertEquals("EmissiveCampania", layerInfo.getName());
        ResourceInfo resource = layerInfo.getResource();
        StoreInfo store = resource.getStore();
        assertEquals(targetWs, store.getWorkspace());
    }

    public void testUploadCoverageNonExistentWorkspace() throws Exception {
        configPersister.setDefaults(getCatalog().getDefaultWorkspace(), null);

        FileItem coverageFileItem = fileItemMock(coverageTestName, coverageTestResource);
        params.put("file", coverageFileItem);
        params.put("workspace", "nonExistentWorkspace");

        try {
            uploader.uploadLayers(params);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("workspace"));
        }
    }

    /**
     * @param uploadFileTestResource
     *            name of the resource to mock as a FileItem that resides under
     *            {@code getClass().getPackage().getName() + "/test-data/}
     */
    private FileItem fileItemMock(final String uploadedFileName, final String uploadFileTestResource)
            throws IOException {
        InputStream in = getClass().getResourceAsStream("test-data/" + uploadFileTestResource);

        return fileMockUp(uploadedFileName, in);
    }

    private FileItem fileMockUp(String uploadFileName, InputStream uploadedData) throws IOException {
        String contentType = "application/zip";
        boolean isFormField = true;
        int sizeThreshold = 1024;
        File repository = new File("target");
        assertTrue(repository.exists());
        assertTrue(repository.isDirectory());
        assertTrue(repository.canWrite());
        // this is how you mock up a FileItem
        FileItem fileItem = new DiskFileItem("file", contentType, isFormField, uploadFileName,
                sizeThreshold, repository);
        OutputStream out = fileItem.getOutputStream();
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        IOUtils.copy(uploadedData, buff);
        out.write(buff.toByteArray());
        return fileItem;
    }

    private WorkspaceInfo getNonDefaultWorkspace() {
        List<WorkspaceInfo> workspaces = getCatalog().getWorkspaces();
        assertTrue(workspaces.size() > 1);
        for (WorkspaceInfo ws : workspaces) {
            if (!ws.equals(getCatalog().getDefaultWorkspace())) {
                return ws;
            }
        }
        throw new IllegalStateException();
    }

    private DataStoreInfo createDirectoryDataStore(final WorkspaceInfo targetWs)
            throws IOException, MalformedURLException {
        final DataStoreInfo targetDs;
        final String dataStoreName = "testDirectoryDS";
        {
            File dsDir = dataDir.findOrCreateDataDir(dataStoreName);
            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            cb.setWorkspace(targetWs);
            DataStoreInfo ds = cb.buildDataStore(dataStoreName);
            // if given a url to a directory it creates a DirectoryDataStore
            ds.getConnectionParameters().put(ShapefileDataStoreFactory.URLP.key,
                    dsDir.toURI().toURL());
            getCatalog().add(ds);
            targetDs = getCatalog().getDataStoreByName(dataStoreName);
            assertNotNull(targetDs);
        }
        return targetDs;
    }
}
