package testsupport;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javax.imageio.ImageIO;
import org.geoserver.printng.FreemarkerSupport;
import static org.junit.Assert.*;
import org.restlet.data.Form;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public final class PrintTestSupport {
    
    private PrintTestSupport() {}
    
    public static Form form(String... kvp) {
        Form form = new Form();
        for (int i = 0; i < kvp.length; i += 2) {
            form.add(kvp[i], kvp[i + 1]);
        }
        return form;
    }
    
    public static void assertTemplateExists(String path) throws IOException {
        File f = new File(FreemarkerSupport.getPrintngTemplateDirectory(), path);
        assertTrue("expected template : " + f.getPath(), f.exists());
    } 
    
    public static void assertPNG(InputStream bytes, int width, int height) {
        BufferedImage read = null;
        try {
            read = ImageIO.read(bytes);
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("Expected image to be read");
        }
        assertEquals(width, read.getWidth());
        assertEquals(height, read.getHeight());
    }
    
    public static void assertPDF(ByteArrayOutputStream baos) throws IOException {
        assertPDF(new ByteArrayInputStream(baos.toByteArray()));
    }
    
    public static void assertPDF(InputStream bytes) throws IOException {
        byte[] magicBytes = new byte[4];
        int read = bytes.read(magicBytes);
        assertEquals(4, read);
        String magic = new String(magicBytes);
        assertEquals("invalid pdf bytes", "%PDF", magic);
    }
    
    public static class LogCollector extends Handler {
        
        public List<LogRecord> records = new ArrayList<LogRecord>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}
        
    }
}
