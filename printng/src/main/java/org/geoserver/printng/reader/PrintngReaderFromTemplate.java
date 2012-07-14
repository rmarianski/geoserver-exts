package org.geoserver.printng.reader;

import org.geoserver.printng.iface.PrintngReader;
import org.geoserver.printng.iface.PrintngReaderFactory;
import org.geoserver.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Request;
import org.restlet.data.Status;

public class PrintngReaderFromTemplate implements PrintngReaderFactory {

    @Override
    public PrintngReader printngReader(Request request) {
        Form form = request.getResourceRef().getQueryAsForm();
        Parameter templateParameter = form.getFirst("template");
        if (templateParameter == null) {
            throw new RestletException("No template found", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        String templateName = templateParameter.getValue();
        return new FreemarkerTemplateReader(templateName, form);
    }

}
