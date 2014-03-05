/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geoserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.di.GeogitModule;
import org.geogit.di.caching.CachingModule;
import org.geogit.geotools.data.GeoGitDataStore;
import org.geogit.geotools.data.GeoGitDataStoreFactory;
import org.geogit.storage.bdbje.JEStorageModule;
import org.geogit.test.integration.RepositoryTestCase;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableList;
import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class WFSIntegrationTest extends WFSTestSupport {

    /** HTTP_GEOGIT_ORG */
    private static final String NAMESPACE = "http://geogit.org";

    private static final String WORKSPACE = "geogit";

    private static final String STORE = "geogitstore";

    private static RepositoryTestCase helper;

    @Before
    public void revert() throws Exception {
        // revertLayer(CiteTestData.ROAD_SEGMENTS);
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        if (helper != null) {
            helper.tearDown();
            helper.repositoryTempFolder.delete();
        }
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // oevrride to avoid creating all the default feature types but call testData.setUp() only
        // instead
        testData.setUp();
    }

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        namespaces.put(WORKSPACE, NAMESPACE);
    }

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {

        helper = new RepositoryTestCase() {

            @Override
            protected Injector createInjector() {
                return Guice.createInjector(Modules.override(new GeogitModule(),
                        new CachingModule()).with(new JEStorageModule()));
            }

            @Override
            protected void setUpInternal() throws Exception {
                configureGeogitDataStore();
            }
        };
        helper.repositoryTempFolder.create();
        helper.setUp();
    }

    private void configureGeogitDataStore() throws Exception {

        helper.insertAndAdd(helper.lines1);
        helper.getGeogit().command(CommitOp.class).call();

        Catalog catalog = getCatalog();
        CatalogFactory factory = catalog.getFactory();
        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix(WORKSPACE);
        ns.setURI(NAMESPACE);
        catalog.add(ns);
        WorkspaceInfo ws = factory.createWorkspace();
        ws.setName(ns.getName());
        catalog.add(ws);

        DataStoreInfo ds = factory.createDataStore();
        ds.setEnabled(true);
        ds.setDescription("Test Geogit DataStore");
        ds.setName(STORE);
        ds.setType(GeoGitDataStoreFactory.DISPLAY_NAME);
        ds.setWorkspace(ws);
        Map<String, Serializable> connParams = ds.getConnectionParameters();

        Optional<URL> geogitDir = helper.getGeogit().command(ResolveGeogitDir.class).call();
        File repositoryUrl = new File(geogitDir.get().toURI()).getParentFile();
        assertTrue(repositoryUrl.exists() && repositoryUrl.isDirectory());

        connParams.put(GeoGitDataStoreFactory.REPOSITORY.key, repositoryUrl);
        connParams.put(GeoGitDataStoreFactory.DEFAULT_NAMESPACE.key, ns.getURI());
        catalog.add(ds);

        DataStoreInfo dsInfo = catalog.getDataStoreByName(WORKSPACE, STORE);
        assertNotNull(dsInfo);
        assertEquals(GeoGitDataStoreFactory.DISPLAY_NAME, dsInfo.getType());
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = dsInfo.getDataStore(null);
        assertNotNull(dataStore);
        assertTrue(dataStore instanceof GeoGitDataStore);

        FeatureTypeInfo fti = factory.createFeatureType();
        fti.setNamespace(ns);
        fti.setCatalog(catalog);
        fti.setStore(dsInfo);
        fti.setSRS("EPSG:4326");
        fti.setName("Lines");
        fti.setAdvertised(true);
        fti.setEnabled(true);
        fti.setFilter(Filter.INCLUDE);
        fti.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        ReferencedEnvelope bounds = new ReferencedEnvelope(-180, 180, -90, 90,
                CRS.decode("EPSG:4326"));
        fti.setNativeBoundingBox(bounds);
        fti.setLatLonBoundingBox(bounds);
        catalog.add(fti);

        fti = catalog.getFeatureType(fti.getId());

        FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
        featureSource = fti.getFeatureSource(null, null);
        assertNotNull(featureSource);
    }

    @Test
    public void testInsert() throws Exception {
        Document dom = insert();
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        assertEquals("1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild()
                .getNodeValue());

        dom = getAsDOM("wfs?version=1.1.0&request=getfeature&typename=geogit:Lines&srsName=EPSG:4326&");
        // print(dom);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        assertEquals(2, dom.getElementsByTagName("geogit:Lines").getLength());
    }

    private Document insert() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "//
                + " xmlns:wfs=\"http://www.opengis.net/wfs\" "//
                + " xmlns:gml=\"http://www.opengis.net/gml\" " //
                + " xmlns:geogit=\"" + NAMESPACE + "\">"//
                + "<wfs:Insert>"//
                + "<geogit:Lines gml:id=\"Lines.1000\">"//
                + "    <geogit:sp>StringProp new</geogit:sp>"//
                + "    <geogit:ip>999</geogit:ip>"//
                + "    <geogit:pp>"//
                + "        <gml:LineString srsDimension=\"2\" srsName=\"EPSG:4326\">"//
                + "            <gml:posList>1.0 1.0 2.0 2.0</gml:posList>"//
                + "        </gml:LineString>"//
                + "    </geogit:pp>"//
                + "</geogit:Lines>"//
                + "</wfs:Insert>"//
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        return dom;
    }

    @Test
    public void testUpdate() throws Exception {
        Document dom = update();
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals("1", getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild()
                .getNodeValue());

        dom = getAsDOM("wfs?version=1.1.0&request=getfeature&typename=geogit:Lines" + "&"
                + "cql_filter=ip%3D1000");
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        assertEquals(1, dom.getElementsByTagName("geogit:Lines").getLength());
    }

    private Document update() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""//
                + " xmlns:geogit=\"" + NAMESPACE
                + "\""//
                + " xmlns:ogc=\"http://www.opengis.net/ogc\""//
                + " xmlns:gml=\"http://www.opengis.net/gml\""//
                + " xmlns:wfs=\"http://www.opengis.net/wfs\">"//
                + " <wfs:Update typeName=\"geogit:Lines\">"//
                + "   <wfs:Property>"//
                + "     <wfs:Name>geogit:pp</wfs:Name>"//
                + "     <wfs:Value>"
                + "        <gml:LineString srsDimension=\"2\" srsName=\"EPSG:4326\">"//
                + "            <gml:posList>1 2 3 4</gml:posList>"//
                + "        </gml:LineString>"//
                + "     </wfs:Value>"//
                + "   </wfs:Property>"//
                + "   <ogc:Filter>"//
                + "     <ogc:PropertyIsEqualTo>"//
                + "       <ogc:PropertyName>ip</ogc:PropertyName>"//
                + "       <ogc:Literal>1000</ogc:Literal>"//
                + "     </ogc:PropertyIsEqualTo>"//
                + "   </ogc:Filter>"//
                + " </wfs:Update>"//
                + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        return dom;
    }

    /**
     * Test case to expose issue https://github.com/opengeo/GeoGit/issues/310
     * "Editing Features changes the feature type"
     * 
     * @see #testUpdateDoesntChangeFeatureType()
     */
    @Test
    public void testInsertDoesntChangeFeatureType() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "//
                + " xmlns:wfs=\"http://www.opengis.net/wfs\" "//
                + " xmlns:gml=\"http://www.opengis.net/gml\" " //
                + " xmlns:geogit=\"" + NAMESPACE + "\">"//
                + "<wfs:Insert>"//
                + "<geogit:Lines gml:id=\"Lines.1000\">"//
                + "    <geogit:sp>added</geogit:sp>"//
                + "    <geogit:ip>7</geogit:ip>"//
                + "    <geogit:pp>"//
                + "        <gml:LineString srsDimension=\"2\" srsName=\"EPSG:4326\">"//
                + "            <gml:posList>1 2 3 4</gml:posList>"//
                + "        </gml:LineString>"//
                + "    </geogit:pp>"//
                + "</geogit:Lines>"//
                + "</wfs:Insert>"//
                + "</wfs:Transaction>";

        GeoGIT geogit = helper.getGeogit();
        final NodeRef initialTypeTreeRef = geogit.command(FindTreeChild.class)
                .setChildPath("Lines").call().get();
        assertFalse(initialTypeTreeRef.getMetadataId().isNull());

        Document dom = postAsDOM("wfs", xml);
        try {
            assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        } catch (AssertionError e) {
            print(dom);
            throw e;
        }

        try {
            assertEquals("1", getFirstElementByTagName(dom, "wfs:totalInserted").getFirstChild()
                    .getNodeValue());
        } catch (AssertionError e) {
            print(dom);
            throw e;
        }

        final NodeRef finalTypeTreeRef = geogit.command(FindTreeChild.class).setChildPath("Lines")
                .call().get();
        assertFalse(initialTypeTreeRef.equals(finalTypeTreeRef));
        assertFalse(finalTypeTreeRef.getMetadataId().isNull());

        assertEquals("Feature type tree metadataId shouuldn't change upon edits",
                initialTypeTreeRef.getMetadataId(), finalTypeTreeRef.getMetadataId());

        Iterator<NodeRef> featureRefs = geogit.command(LsTreeOp.class).setReference("Lines").call();
        while (featureRefs.hasNext()) {
            NodeRef ref = featureRefs.next();
            assertEquals(finalTypeTreeRef.getMetadataId(), ref.getMetadataId());
            assertFalse(ref.toString(), ref.getNode().getMetadataId().isPresent());
        }
    }

    /**
     * Test case to expose issue https://github.com/opengeo/GeoGit/issues/310
     * "Editing Features changes the feature type"
     * 
     * @see #testInsertDoesntChangeFeatureType()
     */
    @Test
    public void testUpdateDoesntChangeFeatureType() throws Exception {
        String xml = "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""//
                + " xmlns:geogit=\"" + NAMESPACE
                + "\""//
                + " xmlns:ogc=\"http://www.opengis.net/ogc\""//
                + " xmlns:gml=\"http://www.opengis.net/gml\""//
                + " xmlns:wfs=\"http://www.opengis.net/wfs\">"//
                + " <wfs:Update typeName=\"geogit:Lines\">"//
                + "   <wfs:Property>"//
                + "     <wfs:Name>geogit:pp</wfs:Name>"//
                + "     <wfs:Value>"
                + "        <gml:LineString srsDimension=\"2\" srsName=\"EPSG:4326\">"//
                + "            <gml:posList>3 4 5 6</gml:posList>"//
                + "        </gml:LineString>"//
                + "     </wfs:Value>"//
                + "   </wfs:Property>"//
                + "   <ogc:Filter>"//
                + "     <ogc:PropertyIsEqualTo>"//
                + "       <ogc:PropertyName>ip</ogc:PropertyName>"//
                + "       <ogc:Literal>1000</ogc:Literal>"//
                + "     </ogc:PropertyIsEqualTo>"//
                + "   </ogc:Filter>"//
                + " </wfs:Update>"//
                + "</wfs:Transaction>";

        GeoGIT geogit = helper.getGeogit();
        final NodeRef initialTypeTreeRef = geogit.command(FindTreeChild.class)
                .setChildPath("Lines").call().get();
        assertFalse(initialTypeTreeRef.getMetadataId().isNull());

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());

        assertEquals("1", getFirstElementByTagName(dom, "wfs:totalUpdated").getFirstChild()
                .getNodeValue());

        final NodeRef finalTypeTreeRef = geogit.command(FindTreeChild.class).setChildPath("Lines")
                .call().get();
        assertFalse(initialTypeTreeRef.equals(finalTypeTreeRef));
        assertFalse(finalTypeTreeRef.getMetadataId().isNull());

        assertEquals("Feature type tree metadataId shouuldn't change upon edits",
                initialTypeTreeRef.getMetadataId(), finalTypeTreeRef.getMetadataId());
        Iterator<NodeRef> featureRefs = geogit.command(LsTreeOp.class).setReference("Lines").call();
        while (featureRefs.hasNext()) {
            NodeRef ref = featureRefs.next();
            assertEquals(finalTypeTreeRef.getMetadataId(), ref.getMetadataId());
            assertFalse(ref.toString(), ref.getNode().getMetadataId().isPresent());
        }
    }

    @Test
    public void testCommitsSurviveShutDown() throws Exception {
        RepositoryTestCase helper = WFSIntegrationTest.helper;
        GeoGIT geogit = helper.getGeogit();

        insert();
        update();

        List<RevCommit> expected = ImmutableList.copyOf(geogit.command(LogOp.class).call());

        File repoDir = helper.repositoryTempFolder.getRoot();
        // shut down server
        super.doTearDownClass();

        geogit = new GeoGIT(repoDir);
        try {
            assertNotNull(geogit.getRepository());
            List<RevCommit> actual = ImmutableList.copyOf(geogit.command(LogOp.class).call());
            assertEquals(expected, actual);
        } finally {
            geogit.close();
        }
    }
}
