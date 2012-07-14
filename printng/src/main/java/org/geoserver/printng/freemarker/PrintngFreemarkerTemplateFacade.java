package org.geoserver.printng.freemarker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;

public class PrintngFreemarkerTemplateFacade {

    public static File getPrintngTemplateDirectory() throws IOException {
        GeoServerDataDirectory dataDir = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        File templateDir = dataDir.findOrCreateDir("printng", "templates");
        return templateDir;
    }

    public static Writer newTemplateWirter(String templateName) throws IOException {
        File directory = PrintngFreemarkerTemplateFacade.getPrintngTemplateDirectory();
        File template = new File(directory, templateName);
        return new FileWriter(template);
    }

}
