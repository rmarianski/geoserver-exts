package org.geoserver.printng.spi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.printng.GeoserverSupport;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.restlet.OutputDescriptor;
import org.geoserver.rest.RestletException;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;

/**
 * The JSON response is a single object that points to a URL where the client
 * can retrieve the rendered output.
 * 
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class JSONWriter extends PrintngWriter {
    
    private final OutputDescriptor outputFormat;
    private final String baseURL;

    public JSONWriter(OutputDescriptor outputDescriptor, String baseURL) {
        this.outputFormat = outputDescriptor;
        this.baseURL = baseURL;
    }

    @Override
    protected void writeInternal(PrintSpec spec, OutputStream out) throws IOException {
        File output = GeoserverSupport.getOutputFile(outputFormat.getExtension());
        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(output));
        IOException error = null;
        try {
            outputFormat.getWriter().write(spec, bout);
            String uri = baseURL + GeoserverSupport.getOutputFileURI(output.getAbsolutePath());
            String response = render(uri);
            out.write(response.getBytes());
        } catch (IOException ioe) {
            error = ioe;
        } finally {
            try {
                bout.close();
            } catch (IOException ioe) {
                // pass
            }
        }
        if (error != null) {
            output.delete(); // try now, it will get cleaned up later otherwise
            
            // the existing mapfish print protocol likes a 500
            throw new RestletException(error.getMessage(), Status.SERVER_ERROR_INTERNAL);
        }
    }
    
    private String render(String output) {
        JSONObject resp = new JSONObject();
        try {
            resp.put("getURL", output);
        } catch (JSONException ex) {
            // this shouldn't happen
            throw new RuntimeException(ex);
        }
        return resp.toString();
    }
}
