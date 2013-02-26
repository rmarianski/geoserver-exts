package org.opengeo.data.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.Importer;
import org.opengeo.data.importer.rest.ImportResource.ImportContextJSONFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

public class DataResource extends BaseResource {


    public DataResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new ImportDataJSONFormat());
    }

    @Override
    public void handleGet() {
        DataFormat formatGet = getFormatGet();
        if (formatGet == null) {
            formatGet = new ImportDataJSONFormat();
        }

        getResponse().setEntity(formatGet.toRepresentation(lookupContext().getData()));
    }

    public class ImportDataJSONFormat extends StreamDataFormat {

        protected ImportDataJSONFormat() {
            super(MediaType.APPLICATION_JSON);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            
            return new ImportJSONIO(importer).data(in);
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            new ImportJSONIO(importer).data((ImportData)object, getPageInfo(), out);
        }
    
    }
}
