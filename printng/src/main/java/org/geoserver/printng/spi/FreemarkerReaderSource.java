package org.geoserver.printng.spi;

import java.util.Set;

import org.geoserver.printng.api.PrintngReader;
import org.geoserver.printng.api.ReaderSource;
import org.geoserver.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Status;

import freemarker.template.SimpleHash;
import java.io.IOException;
import net.sf.json.JSONObject;
import org.restlet.data.MediaType;

public class FreemarkerReaderSource implements ReaderSource {

    @Override
    public PrintngReader printngReader(Request request) throws IOException {
        String templateName = request.getAttributes().get("template").toString();
        if (templateName == null) {
            throw new RestletException("No template found", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        SimpleHash simpleHash = new SimpleHash();
        addFromForm(simpleHash, request.getResourceRef().getQueryAsForm());
        if (request.getEntity().getMediaType() == MediaType.APPLICATION_JSON) {
            JSONObject json = JSONObject.fromObject(request.getEntity().getText());
            addFromJSON(simpleHash, json);
        } else {
            Form post = request.getEntityAsForm();
            if (post != null) {
                addFromForm(simpleHash, post);
            }
        }
        return new FreemarkerReader(templateName, simpleHash);
    }

    private void addFromForm(SimpleHash hash, Form form) {
        Set<String> names = form.getNames();
        for (String name : names) {
            String value = form.getFirst(name).getValue();
            hash.put(name, value);
        }
    }

    private void addFromJSON(SimpleHash parent, JSONObject json) {
        for (Object key : json.keySet()) {
            Object obj = json.get(key);
            if (obj instanceof JSONObject) {
                SimpleHash child = new SimpleHash();
                parent.put(obj.toString(), child);
                addFromJSON(child, (JSONObject) obj);
            } else {
                parent.put(key.toString(), obj);
            }
        }
    }
}
