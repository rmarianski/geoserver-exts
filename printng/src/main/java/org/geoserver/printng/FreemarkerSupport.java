package org.geoserver.printng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;

public class FreemarkerSupport {
    
    public static File getPrintngTemplateDirectory() throws IOException {
        GeoServerDataDirectory dataDir = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        File templateDir;
        if (dataDir == null) {
            templateDir = new File(System.getProperty("java.io.tmpdir"),"printng");
            templateDir.mkdirs();
        } else {
            templateDir = dataDir.findOrCreateDir("printng", "templates");
        }
        return templateDir;
    }

    public static Writer newTemplateWriter(String templateName) throws IOException {
        File directory = FreemarkerSupport.getPrintngTemplateDirectory();
        File template = new File(directory, templateName);
        return new FileWriter(template);
    }

}
