package org.opengeo.geoserver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

public class StartCluster {

    public static void main(String[] args) throws Exception {
        System.setProperty("GWC_METASTORE_DISABLED", "true");
        System.setProperty("GWC_DISKQUOTA_DISABLED", "true");

        for (int i = 0; i < 2; i++) {
            startJettyServer("jetty"+i, 8000 + i);
        }
    }

    static Server startJettyServer(String name, int port) throws Exception {

        Server jettyServer = new Server();

        SocketConnector conn = new SocketConnector();
        conn.setPort(port);
        conn.setAcceptQueueSize(100);
        conn.setMaxIdleTime(1000 * 60 * 60);
        conn.setSoLingerTime(-1);

        WebAppContext wah = new WebAppContext();
        wah.setContextPath("/geoserver");
        wah.setWar("src/main/webapp");

        //create a node specific base directory
        File nodeDir = new File("target/" + name); 
        nodeDir.mkdirs();

        wah.setTempDirectory(new File(nodeDir, "work"));

        //this allows to send large SLD's from the styles form
        wah.getServletContext().getContextHandler().setMaxFormContentSize(1024 * 1024 * 2);

        File logDir = new File(nodeDir, "logs"); 
        logDir.mkdirs();

        Map initParams = new HashMap();
        initParams.put("GEOSERVER_LOG_LOCATION", logDir.getPath() + "/geoserver.log");
        wah.setInitParams(initParams);

        jettyServer.setHandler(wah);
        jettyServer.setConnectors(new Connector[] { conn });
        jettyServer.start();
        return jettyServer;
    }
}
