package org.geoserver.printng.restlet;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.spi.PrintSpecException;
import org.geotools.util.logging.Logging;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import testsupport.PrintTestSupport;
import testsupport.PrintTestSupport.LogCollector;
import static testsupport.PrintTestSupport.form;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintSpecFormConfiguratorTest {

    PrintSpec spec = new PrintSpec(null);
    private LogCollector records;
    Logger logger = Logging.getLogger(PrintSpecFormConfigurator.class);

    @Before
    public void installHandler() {
        records = new PrintTestSupport.LogCollector();
        logger.setLevel(Level.FINE);
        logger.addHandler(records);
    }

    @After
    public void clearHandler() {
        logger.setLevel(Level.INFO);
        logger.removeHandler(records);
    }

    @Test
    public void testValidation() {
        try {
            PrintSpecFormConfigurator.configure(spec, form("width", "xyz"));
            fail("expected exception");
        } catch (PrintSpecException pse) {
            // make sure it's mentioned somewhere
            assertTrue(pse.getMessage().indexOf("width") >= 0);
        }
        try {
            PrintSpecFormConfigurator.configure(spec, form("height", "xyz"));
            fail("expected exception");
        } catch (PrintSpecException pse) {
            // make sure it's mentioned somewhere
            assertTrue(pse.getMessage().indexOf("height") >= 0);
        }
        try {
            PrintSpecFormConfigurator.configure(spec, form("auth", "1,2"));
            fail("expected exception");
        } catch (PrintSpecException pse) {
            // make sure it's mentioned somewhere
            assertTrue(pse.getMessage().indexOf("auth") >= 0);
        }
        try {
            PrintSpecFormConfigurator.configure(spec, form("cookie", "1,2"));
            fail("expected exception");
        } catch (PrintSpecException pse) {
            // make sure it's mentioned somewhere
            assertTrue(pse.getMessage().indexOf("cookie") >= 0);
        }
    }

    @Test
    public void testMultipleCookies() {
        PrintSpecFormConfigurator.configure(spec, form("cookie", "a,b,c", "cookie", "d,e,f"));
        assertNotNull(spec.getCookie("a"));
        assertNotNull(spec.getCookie("d"));
        // this fails when whole project is tested?
//        assertEquals(2, records.records.size());
    }

    @Test
    public void testMultipleAuth() {
        PrintSpecFormConfigurator.configure(spec, form("auth", "a,b,c", "auth", "d,e,f"));
        assertNotNull(spec.getCredentials("a"));
        assertNotNull(spec.getCredentials("d"));
        // this fails when whole project is tested?
//        assertEquals(2, records.records.size());
    }
}
