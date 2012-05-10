package org.opengeo.analytics;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.data.util.IOUtils;
import org.geoserver.monitor.hib.MonitoringDataSource;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.VirtualTable;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;
import org.geotools.jdbc.RegexpValidator;
import org.geotools.jdbc.VirtualTableParameter;

public class RequestMapInitializer implements GeoServerInitializer {

    static Logger LOGGER = Logging.getLogger("org.opengeo.analytics");
    
    static String START_END_REGEXP = "start_time > '[^();]+' and end_time < '[^();]+'";
    MonitoringDataSource dataSource;
    
    public RequestMapInitializer(MonitoringDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void initialize(GeoServer geoServer) throws Exception {
        Catalog cat = geoServer.getCatalog();
        
        //set up a workspace
        WorkspaceInfo ws = null;
        if ((ws = cat.getWorkspaceByName("analytics")) == null) {
            ws = cat.getFactory().createWorkspace();
            ws.setName("analytics");
            cat.add(ws);
                
            NamespaceInfo ns = cat.getFactory().createNamespace();
            ns.setPrefix("analytics");
            ns.setURI("http://opengeo.org/analytics");
            cat.add(ns);
        }
        
        //set up the styles
        addStyle(cat, "analytics_requests_agg");
        
        //setup data files
        GeoServerResourceLoader rl = cat.getResourceLoader();
//        File f = rl.find("data", "monitor", "monitor_world.shp");
//        if (f == null) {
//            File dir = rl.findOrCreateDirectory("data", "monitor");
//            IOUtils.decompress(getClass().getResourceAsStream("monitor_world.zip"), dir);
//            f = rl.find("data", "monitor", "monitor_world.shp");
//        }
//        
//        //setup a datastore for the base layer
//        if (cat.getDataStoreByName("monitor", "monitor_world") == null) {
//            DataStoreInfo ds = cat.getFactory().createDataStore();
//            ds.setWorkspace(ws);
//            ds.setName("monitor_world");
//            ds.getConnectionParameters().put(ShapefileDataStoreFactory.URLP.key, f.toURL());
//            ds.setEnabled(true);
//            cat.add(ds);
//            
//            FeatureTypeInfo ft = cat.getFactory().createFeatureType();
//            ft.setName("monitor_world");
//            ft.setNativeName("monitor_world");
//            ft.setNamespace(cat.getNamespaceByPrefix(ws.getName()));
//            ft.setStore(ds);
//            ft.setEnabled(true);
//            setWorldBounds(ft);
//         
//            cat.add(ft);
//            
//            addLayer(cat, ft, "monitor_world");
//        }
        
        //set up a datastore for the request layer 
        DataStoreInfo ds = cat.getDataStoreByName("analytics", "db");
        if ( ds == null) {
            ds = cat.getFactory().createDataStore();
            ds.setWorkspace(ws);
            ds.setName("db");
            ds.setEnabled(true);
            updateParams(ds);
            
            cat.add(ds);
        }
        else {
            updateParams(ds);
            cat.save(ds);
        }
        
        FeatureTypeInfo ft = cat.getFeatureTypeByDataStore(ds, "requests_agg");
        if (ft == null) {
            ft = cat.getFactory().createFeatureType();
            ft.setName("requests_agg");
            ft.setNativeName("requests_agg");
            ft.setNamespace(cat.getNamespaceByPrefix(ws.getName()));
            ft.setStore(ds);
            ft.setEnabled(true);
            setWorldBounds(ft);
            ft.getMetadata().put("JDBC_VIRTUAL_TABLE", createAggRequestsVirtualTable());
            cat.add(ft);
        } else {
            VirtualTable vt = (VirtualTable) ft.getMetadata().get("JDBC_VIRTUAL_TABLE");
            // check for updates - prior to 2.4.4, there was no parameter
            if (vt.getParameter("query") == null) {
                ft.getMetadata().put("JDBC_VIRTUAL_TABLE", createAggRequestsVirtualTable());
            }
            cat.save(ft);
        }
        
        if (cat.getLayers(ft).isEmpty()) {
            addLayer(cat, ft, "analytics_requests_agg");
        }
        
        ft = cat.getFeatureTypeByDataStore(ds, "requests");
        if (ft == null) {
            ft = cat.getFactory().createFeatureType();
            ft.setName("requests");
            ft.setNativeName("requests");
            ft.setNamespace(cat.getNamespaceByPrefix(ws.getName()));
            ft.setStore(ds);
            ft.setEnabled(true);
            setWorldBounds(ft);
            
            VirtualTable vt = new VirtualTable("requests", 
                "SELECT ST_SetSRID(ST_MakePoint(remote_lon,remote_lat), 4326) as \"point\"," +
                       "id as \"ID\", " +
                       "id as \"REQUEST_ID\", " +
                       "status as \"STATUS\", " +
                       "category as \"CATEGORY\", " +
                       "path as \"PATH\", " +
                       "query_string as \"QUERY_STRING\", " +
                       "body_content_type as \"BODY_CONTENT_TYPE\", " +
                       "body_content_length as \"BODY_CONTENT_LENGTH\", " +
                       "server_host as \"SERVER_HOST\", " +
                       "http_method as \"HTTP_METHOD\", " +
                       "start_time as \"START_TIME\", " +
                       "end_time as \"END_TIME\", " +
                       "total_time as \"TOTAL_TIME\", " +
                       "remote_address as \"REMOTE_ADDRESS\", " +
                       "remote_host as \"REMOTE_HOST\", " +
                       "remote_user as \"REMOTE_USER\", " +
                       "remote_country as \"REMOTE_COUNTRY\", " +
                       "remote_city as \"REMOTE_CITY\", " +
                       "service as \"SERVICE\", " +
                       "operation as \"OPERATION\", " +
                       "sub_operation as \"SUB_OPERATION\", " +
                       "ows_version as \"OWS_VERSION\", " +
                       "content_type as \"CONTENT_TYPE\", " +
                       "response_length as \"RESPONSE_LENGTH\", " +
                       "error_message as \"ERROR_MESSAGE\"" + 
                 " FROM request");
            
            vt.addGeometryMetadatata("POINT", Point.class, 4326);
            vt.setPrimaryKeyColumns(Arrays.asList("ID"));
            
            ft.getMetadata().put("JDBC_VIRTUAL_TABLE", vt);
            cat.add(ft);
        }
        
        if (cat.getLayers(ft).isEmpty()) {
            addLayer(cat, ft, "point");
        }
    }
    
    VirtualTable createAggRequestsVirtualTable() {
        VirtualTable vt = new VirtualTable("requests_agg", 
            "SELECT ST_SetSRID(ST_MakePoint(remote_lon,remote_lat), 4326) as \"POINT\"," +
                    "remote_city as \"REMOTE_CITY\", " +
                    "remote_country as \"REMOTE_COUNTRY\", " +
                    "count(*) as \"REQUESTS\"" + 
             " FROM request WHERE remote_lon <> 0 and remote_lat <> 0 and %query%" + 
          "GROUP BY \"POINT\", \"REMOTE_CITY\", \"REMOTE_COUNTRY\"");
        vt.addGeometryMetadatata("POINT", Point.class, 4326);
        RegexpValidator validator = new RegexpValidator(START_END_REGEXP);
        vt.addParameter(new VirtualTableParameter("query","1=1",validator));
        return vt;
    }
    
    void addStyle(Catalog cat, String name) throws Exception {
        GeoServerResourceLoader rl = cat.getResourceLoader();
        if (cat.getStyleByName(name) == null) {
            File sld = rl.find("styles", name + ".sld");
            if (sld == null) {
                rl.copyFromClassPath(name + ".sld",
                    rl.createFile("styles", name + ".sld"), getClass());
            }
            
            StyleInfo s = cat.getFactory().createStyle();
            s.setName(name);
            s.setFilename(name + ".sld");
            cat.add(s);
        }
    }

    void updateParams(DataStoreInfo ds) {
        Map params = new HashMap();
        
        setParamsFromURL(params, dataSource.getUrl());
        params.put(JDBCDataStoreFactory.USER.key, dataSource.getUsername());
        params.put(JDBCDataStoreFactory.PASSWD.key, dataSource.getPassword());
        
        ds.getConnectionParameters().putAll(params);
    }
    
    void setParamsFromURL(Map params, String url) {
        if (url.startsWith("jdbc:postgresql:")) {
            //jdbc:postgresql://localhost/geotools
            int i = url.indexOf("//") + 2;
            int j = url.indexOf('/', i);
            
            String host = url.substring(i, j);
            int port = -1;
            if (host.contains(":")) {
                port = Integer.parseInt(host.substring(host.indexOf(':')+1));
                host = host.substring(0, host.indexOf(':'));
            }
            
            String db = url.substring(j+1);
            params.put(JDBCDataStoreFactory.DBTYPE.key, "postgis");
            params.put(JDBCDataStoreFactory.HOST.key, host);
            if (port != -1) {
                params.put(JDBCDataStoreFactory.PORT.key, port);
            }
            params.put(JDBCDataStoreFactory.DATABASE.key, db);
        }
        else if (url.startsWith("jdbc:h2:file:")) {
            //jdbc:h2:file:/path
            params.put(JDBCDataStoreFactory.DBTYPE.key, "h2");
            params.put(JDBCDataStoreFactory.DATABASE.key, url.substring("jdbc:h2:file:".length()));
        }
        else {
            LOGGER.warning("Unable to configure analytics:db store from jdbc url " + url);
        }
    }
    
    void setWorldBounds(FeatureTypeInfo ft) throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        ft.setLatLonBoundingBox(new ReferencedEnvelope(-180, 180,-90, 90, crs));
        ft.setNativeBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, crs));
        ft.setNativeCRS(crs);
        ft.setSRS("EPSG:4326");
        ft.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
    }
    
    ReferencedEnvelope worldBounds() throws Exception {
        return new ReferencedEnvelope(-180, 180,-90, 90, CRS.decode("EPSG:4326"));
    }
    
    void addLayer(Catalog cat, FeatureTypeInfo ft, String style) {
        LayerInfo l = cat.getFactory().createLayer();
        l.setResource(ft);
        l.setType(LayerInfo.Type.VECTOR);
        l.setEnabled(true);
        l.setDefaultStyle(cat.getStyleByName(style));
        
        cat.add(l);
    }
}
