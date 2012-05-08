/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.data.importer.web;

import static org.geotools.data.postgis.PostgisNGDataStoreFactory.LOOSEBBOX;
import static org.geotools.data.postgis.PostgisNGDataStoreFactory.PREPARED_STATEMENTS;
import static org.geotools.jdbc.JDBCDataStoreFactory.DATABASE;
import static org.geotools.jdbc.JDBCDataStoreFactory.FETCHSIZE;
import static org.geotools.jdbc.JDBCDataStoreFactory.HOST;
import static org.geotools.jdbc.JDBCDataStoreFactory.MAXCONN;
import static org.geotools.jdbc.JDBCDataStoreFactory.MAXWAIT;
import static org.geotools.jdbc.JDBCDataStoreFactory.MINCONN;
import static org.geotools.jdbc.JDBCDataStoreFactory.PASSWD;
import static org.geotools.jdbc.JDBCDataStoreFactory.PK_METADATA_TABLE;
import static org.geotools.jdbc.JDBCDataStoreFactory.USER;
import static org.geotools.jdbc.JDBCDataStoreFactory.VALIDATECONN;
import static org.geotools.jdbc.JDBCJNDIDataStoreFactory.JNDI_REFNAME;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.postgis.PostgisNGJNDIDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

/**
 * Configuration panel for PostGIS.
 * 
 * @author Andrea Aime - OpenGeo
 */
public class PostGISPanel extends AbstractDbPanel {
    
    JNDIDbParamPanel jndiParamPanel;
    BasicDbParamPanel basicParamPanel;

    public PostGISPanel(String id) {
        super(id);
    }

    @Override
    protected LinkedHashMap<String, Component> buildParamPanels() {
        LinkedHashMap<String, Component> result = new LinkedHashMap<String, Component>();

        //
        // suite postgis defaults:
        //  port = 54321
        //  database = username = <current user> 
        //
        int port = 54321;
        String db = System.getProperty("user.name");
        String user = db;
        
        // basic panel
        basicParamPanel = new BasicDbParamPanel("01", "localhost", port, db, "public", user, true);
        result.put(CONNECTION_DEFAULT, basicParamPanel);

        // jndi panel
        jndiParamPanel = new JNDIDbParamPanel("02", "java:comp/env/jdbc/mydatabase");
        result.put(CONNECTION_JNDI, jndiParamPanel);
        
        return result;
    }
    
    @Override
    protected DataStoreFactorySpi fillStoreParams(Map<String, Serializable> params) {
        DataStoreFactorySpi factory;
        params.put(JDBCDataStoreFactory.DBTYPE.key, (String) PostgisNGDataStoreFactory.DBTYPE.sample);
        if (CONNECTION_JNDI.equals(connectionType)) {
            factory = new PostgisNGJNDIDataStoreFactory();

            params.put(JNDI_REFNAME.key, jndiParamPanel.jndiReferenceName);
            params.put(JDBCDataStoreFactory.SCHEMA.key, jndiParamPanel.schema);
        } 
        else {
            factory = new PostgisNGDataStoreFactory();

            // basic params
            params.put(HOST.key, basicParamPanel.host);
            params.put(PostgisNGDataStoreFactory.PORT.key, basicParamPanel.port);
            params.put(USER.key, basicParamPanel.username);
            params.put(PASSWD.key, basicParamPanel.password);
            params.put(DATABASE.key, basicParamPanel.database);
            params.put(JDBCDataStoreFactory.SCHEMA.key, basicParamPanel.schema);

            // connection pool params
            params.put(MINCONN.key, basicParamPanel.connPoolPanel.minConnection);
            params.put(MAXCONN.key, basicParamPanel.connPoolPanel.maxConnection);
            params.put(FETCHSIZE.key, basicParamPanel.connPoolPanel.fetchSize);
            params.put(MAXWAIT.key, basicParamPanel.connPoolPanel.timeout);
            params.put(VALIDATECONN.key, basicParamPanel.connPoolPanel.validate);
            params.put(PREPARED_STATEMENTS.key, basicParamPanel.connPoolPanel.preparedStatements);
        }

        //advanced
        //params.put(NAMESPACE.key, new URI(namespace.getURI()).toString());
        params.put(LOOSEBBOX.key, advancedParamPanel.looseBBox);
        params.put(PK_METADATA_TABLE.key, advancedParamPanel.pkMetadata);
        
        return factory;
    }
    
    @Override
    protected AdvancedDbParamPanel buildAdvancedPanel(String id) {
        return new AdvancedDbParamPanel(id, true);
    }

}
