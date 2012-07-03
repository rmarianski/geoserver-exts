/*
 */
package org.geoserver.printng;

import com.mockrunner.mock.web.MockHttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class TemplatePrintResourceTest extends PrintTestSupport {
    
    public void testTemplatePrint() throws Exception {
        File dir = getDataDirectory().findOrCreateDir("printng/templates");
        IOUtils.write("<div>${foo}</div>", new FileOutputStream(new File(dir,"test1.ftl")));
        
        MockHttpServletResponse response = getAsServletResponse("/rest/printng/maps/test1.png?foo=FOOBAR");
        showResult = true;
        // @todo hard-coded width/height
        checkImage(response,"image/png", 800, 600);
    }
}
