package org.opengeo.data.importer;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geotools.data.jdbc.JDBCUtils;

public class OracleImportTest extends ImporterDbTestSupport {

    @Override
    protected String getFixtureId() {
        return "oracle";
    }

    @Override
    protected void doSetUpInternal() throws Exception {
        Connection cx = getConnection();
        try {
            Statement st = cx.createStatement();
            try {
                setUpWidgetsTable(st);
                dropTable("ARCHSITES", st);
                dropTable("BUGSITES", st);
            }
            finally {
                JDBCUtils.close(st);
            }
        }
        finally {
            JDBCUtils.close(cx, null, null);
        }
    }

    void setUpWidgetsTable(Statement st) throws Exception {
        runSafe("DROP TABLE WIDGETS PURGE", st);
        runSafe("DROP SEQUENCE WIDGETS_PKEY_SEQ", st);
        run("DELETE FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = 'WIDGETS'", st);

        String sql = 
            "CREATE TABLE WIDGETS (" +  
              "ID INT, GEOMETRY MDSYS.SDO_GEOMETRY, " + 
              "PRICE FLOAT, DESCRIPTION VARCHAR(255), " + 
              "PRIMARY KEY(id))";
        run(sql, st);
        
        sql = "CREATE SEQUENCE WIDGETS_PKEY_SEQ";
        run(sql, st);
            
        sql = 
            "INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID ) " + 
             "VALUES ('WIDGETS','GEOMETRY', MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X',-180,180,0.5), " + 
             "MDSYS.SDO_DIM_ELEMENT('Y',-90,90,0.5)), 4326)";
        run(sql, st);

        sql = 
            "CREATE INDEX WIDGETS_GEOMETRY_IDX ON WIDGETS(GEOMETRY) " + 
             "INDEXTYPE IS MDSYS.SPATIAL_INDEX PARAMETERS ('SDO_INDX_DIMS=2 LAYER_GTYPE=\"POINT\"')";
        run(sql, st);
            
        sql = "INSERT INTO WIDGETS VALUES (0," +
            "MDSYS.SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(0.0,0.0,NULL),NULL,NULL), 1.99, 'anvil')";
        run(sql, st);

        sql = "INSERT INTO WIDGETS VALUES (1," + 
            "MDSYS.SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(1.0,1.0,NULL),NULL,NULL), 2.99, 'bomb')";
        run(sql, st);

        sql = "INSERT INTO WIDGETS VALUES (2," + 
            "MDSYS.SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(2.0,2.0,NULL),NULL,NULL), 3.99, 'dynamite')";
        run(sql, st);
    }

    void dropTable(String tableName, Statement st) throws Exception {
        runSafe("DROP TABLE " + tableName + " PURGE", st);
        run("DELETE FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = '" +  tableName + "'", st);
    }
    
    public void testDirectImport() throws Exception {
        Database db = new Database(getConnectionParams());

        ImportContext context = importer.createContext(db);
        assertEquals(1, context.getTasks().size());
        
        ImportTask task = context.getTasks().get(0);
        assertEquals(1, task.getItems().size());

        importer.run(context);
        runChecks("gs:WIDGETS");
    }

    public void testIndirectToShapefile() throws Exception {
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        
        ImportContext context = importer.createContext(new Directory(dir));
        importer.run(context);

        runChecks("gs:archsites");
        runChecks("gs:bugsites");

        DataStoreInfo store = (DataStoreInfo) context.getTasks().get(0).getStore();
        assertNotNull(store);
        assertEquals(2, getCatalog().getFeatureTypesByDataStore(store).size());

        context = importer.createContext(new Database(getConnectionParams()), store);
        assertEquals(1, context.getTasks().size());
        
        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals(1, task.getItems().size());
        
        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());

        assertEquals(3, getCatalog().getFeatureTypesByDataStore(store).size());
        runChecks("gs:WIDGETS");
    }

    public void testIndirectToOracle() throws Exception {
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("oracle");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);
        ds.getConnectionParameters().putAll(getConnectionParams());
        cat.add(ds);

        assertEquals(0, cat.getFeatureTypesByDataStore(ds).size());
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        ImportContext context = importer.createContext(new Directory(dir), ds);
        assertEquals(1, context.getTasks().size());
        
        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals(2, task.getItems().size());

        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());

        assertEquals(2, cat.getFeatureTypesByDataStore(ds).size());
        runChecks("gs:ARCHSITES");
        runChecks("gs:BUGSITES");
    }
}
