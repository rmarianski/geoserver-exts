package org.geoserver.printng.reader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;

import freemarker.template.SimpleHash;

public class FreemarkerTemplateReaderTest extends GeoServerTestSupport {

    @Test
    public void testReaderNotFound() throws IOException {
        FreemarkerTemplateReader templateReader = new FreemarkerTemplateReader("foo", null);
        try {
            templateReader.reader();
        } catch (IOException e) {
            assertTrue(true);
            return;
        }
        fail("Expecting IOException to be thrown");
    }

    @Test
    public void testReaderFound() throws IOException {
        SimpleHash simpleHash = new SimpleHash();
        simpleHash.put("quux", "morx");
        FreemarkerTemplateReader freemarkerTemplateReader = new FreemarkerTemplateReader("foo",
                simpleHash);
        createTemplate("foo", new StringReader("<div>foobar</div>"));
        Reader reader = freemarkerTemplateReader.reader();
        String result = IOUtils.toString(reader);
        assertEquals("<div>foobar</div>", result);
    }

    private void createTemplate(String templateName, Reader inputReader) throws IOException {
        GeoServerDataDirectory geoServerDataDirectory = GeoServerExtensions
                .bean(GeoServerDataDirectory.class);
        File templateDir = geoServerDataDirectory.findOrCreateDir("printng", "templates");
        File template = new File(templateDir, templateName);
        FileWriter fileWriter = new FileWriter(template);
        IOUtils.copy(inputReader, fileWriter);
        fileWriter.close();
    }

}
