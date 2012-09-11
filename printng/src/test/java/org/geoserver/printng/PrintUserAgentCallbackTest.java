package org.geoserver.printng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.spi.ParsedDocument;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import testsupport.HTTPD;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintUserAgentCallbackTest {

    static Server server;
    
    @Before
    public void clear() {
        server.requestHeaders.clear();
    }

    @BeforeClass
    public static void startServer() {
        server = new Server();
    }

    @AfterClass
    public static void stopServer() {
        server.stop();
    }

    @Test
    public void testBase() throws IOException {
        PrintSpec spec = new PrintSpec(ParsedDocument.parse(
                String.format("<img src='http://localhost:%s/foobar.png'>", server.getPort())));
        PrintUserAgentCallback callback = new PrintUserAgentCallback(spec, new NaiveUserAgent());
        callback.preload();
        assertEquals(1, server.requestHeaders.size());
    }
    
    @Test
    public void testCookies() throws IOException {
        PrintSpec spec = new PrintSpec(ParsedDocument.parse(
                String.format("<img src='http://localhost:%s/foobar.png'>", server.getPort())));
        spec.addCookie("localhost", "foo", "bar");
        PrintUserAgentCallback callback = new PrintUserAgentCallback(spec, new NaiveUserAgent());
        callback.preload();
        assertEquals(1, server.requestHeaders.size());
        Properties props = server.requestHeaders.get(0);
        assertEquals("foo=bar", props.getProperty("cookie"));
    }
    
    @Test
    public void testCreds() throws IOException {
        PrintSpec spec = new PrintSpec(ParsedDocument.parse(
                String.format("<img src='http://localhost:%s/foobar.png'>", server.getPort())));
        spec.addCredentials("localhost", "foo", "bar");
        PrintUserAgentCallback callback = new PrintUserAgentCallback(spec, new NaiveUserAgent());
        callback.preload();
        assertEquals(1, server.requestHeaders.size());
        Properties props = server.requestHeaders.get(0);
        assertEquals("Basic Zm9vOmJhcg==", props.get("authorization"));
    }

    static class Server extends HTTPD {
        
        List<Properties> requestHeaders = new ArrayList<Properties>();
        
        @Override
        protected void serve(String uri, String method, Properties header, Properties parms) {
            requestHeaders.add(header);
            sendResponse("404", "text/plan", new Properties(), "ERROR");
        }
    }
}
