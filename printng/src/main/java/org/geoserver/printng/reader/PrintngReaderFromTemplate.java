package org.geoserver.printng.reader;

import java.util.Set;

import org.geoserver.printng.iface.PrintngReader;
import org.geoserver.printng.iface.PrintngReaderFactory;
import org.geoserver.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Status;

import freemarker.template.SimpleHash;

public class PrintngReaderFromTemplate implements PrintngReaderFactory {

    @Override
    public PrintngReader printngReader(Request request) {
        String templateName = request.getAttributes().get("template").toString();
        if (templateName == null) {
            throw new RestletException("No template found", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        Form form = request.getResourceRef().getQueryAsForm();
        SimpleHash simpleHash = new SimpleHash();
        Set<String> names = form.getNames();
        for (String name : names) {
            String value = form.getFirst(name).toString();
            simpleHash.put(name, value);
        }
        return new FreemarkerTemplateReader(templateName, simpleHash);
    }

}
