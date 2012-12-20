package org.geoserver.printng.spi;

import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.printng.FreemarkerReaderTest;
import org.junit.Test;
import static org.junit.Assert.*;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class FreemarkerReaderSourceTest {
    
    @Test
    public void testJSON() throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("title", "TITLE");
        JSONArray stuff = new JSONArray();
        for (int i = 0; i < 10; i++) {
            stuff.add(i);
        }
        obj.put("stuff", stuff);
        stuff = new JSONArray();
        for (int i = 0; i < 10; i++) {
            JSONObject s = new JSONObject();
            s.put("name", i);
            stuff.add(s);
        }
        obj.put("moreStuff", stuff);
        obj.put("missing", null);
        
        FreemarkerReaderTest.createTemplate("farby.ftl", new StringReader("${title}"
                + " <#list stuff as x>${x}</#list> "
                + " <#list moreStuff as s>${s.name}</#list>"
                + " ${missing!\"MISSING\"}"));
        
        FreemarkerReaderSource src = new FreemarkerReaderSource();
        Request req = new Request();
        Reference reference = new Reference();
        reference.setPath("/unused");
        reference.setQuery("");
        req.setResourceRef(reference);
        req.getAttributes().put("template", "farby");
        String json = obj.toString();
        req.setEntity(json, MediaType.APPLICATION_JSON);
        String text = new Scanner(src.printngReader(req).reader()).useDelimiter("\\Z").next();
        assertEquals("TITLE 0123456789  0123456789 MISSING", text);
    }
}
