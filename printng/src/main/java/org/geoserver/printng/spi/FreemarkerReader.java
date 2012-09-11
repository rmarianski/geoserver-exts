package org.geoserver.printng.spi;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.geoserver.printng.FreemarkerSupport;
import org.geoserver.printng.api.PrintngReader;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerReader implements PrintngReader {
    
    private final String result;

    public FreemarkerReader(String templateName, SimpleHash templateModel) throws IOException {
        Template template = findTemplate(templateName);
        if (template == null) {
            throw new IOException("Template not found " + templateName);
        }
        StringWriter writer = new StringWriter();
        try {
            template.process(templateModel, writer);
        } catch (TemplateException e) {
            throw new IOException("Error processing template: " + templateName, e);
        }
        this.result = writer.toString();
    }

    @Override
    public Reader reader() throws IOException {
        return new StringReader(result);
    }

    private Template findTemplate(String templateName) throws IOException {
        File templateDirectory = FreemarkerSupport.getPrintngTemplateDirectory();
        Configuration configuration = new Configuration();
        configuration.setDirectoryForTemplateLoading(templateDirectory);
        Template template;
        try {
            template = configuration.getTemplate(templateName);
        } catch (IOException e) {
            if (!templateName.endsWith(".ftl")) {
                return findTemplate(templateName + ".ftl");
            } else {
                throw e;
            }
        }
        return template;
    }

}
