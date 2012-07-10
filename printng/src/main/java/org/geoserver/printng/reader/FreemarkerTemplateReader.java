package org.geoserver.printng.reader;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.printng.iface.PrintngReader;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerTemplateReader implements PrintngReader {

    private final String templateName;

    private final Object templateModel;

    public FreemarkerTemplateReader(String templateName, Object templateModel) {
        this.templateName = templateName;
        this.templateModel = templateModel;
    }

    @Override
    public Reader reader() throws IOException {
        GeoServerDataDirectory geoServerDataDirectory = GeoServerExtensions
                .bean(GeoServerDataDirectory.class);
        File templateDirectory = geoServerDataDirectory.findDataDir("printng", "templates");
        if (templateDirectory == null) {
            throw new IOException("Error finding printng freemarker templates");
        }
        Configuration configuration = new Configuration();
        configuration.setDirectoryForTemplateLoading(templateDirectory);
        Template template = configuration.getTemplate(templateName);
        StringWriter writer = new StringWriter();
        try {
            template.process(templateModel, writer);
        } catch (TemplateException e) {
            throw new IOException("Error processing template: " + templateName, e);
        }
        String result = writer.toString();
        return new StringReader(result);
    }

}
