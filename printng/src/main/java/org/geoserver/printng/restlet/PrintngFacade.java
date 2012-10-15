package org.geoserver.printng.restlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngReader;
import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.api.ReaderSource;
import org.geoserver.printng.spi.ParsedDocument;
import org.geoserver.printng.spi.PrintSpecException;
import org.geoserver.rest.RestletException;
import org.geotools.util.logging.Logging;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Variant;

public class PrintngFacade {

    private final Request request;

    private final Response response;

    private final ReaderSource readerSource;

    private final OutputDescriptor outputDescriptor;

    public PrintngFacade(Request request, Response response, ReaderSource readerSource) {
        this.request = request;
        this.response = response;
        this.readerSource = readerSource;

        String extension = request.getAttributes().get("ext").toString().toLowerCase();
        outputDescriptor = new OutputDescriptor(extension);
    }

    public PrintResource getResource() {
        return new PrintResource(this);
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public Variant getVariant() {
        return outputDescriptor.getVariant();
    }

    public PrintRepresentation getRepresentation(Variant variant) {
        return new PrintRepresentation(this);
    }

    public MediaType getMediaType() {
        return getVariant().getMediaType();
    }

    public PrintngWriter getWriter() {
        return outputDescriptor.getWriter();
    }

    public PrintSpec getPrintSpec() throws IOException {
        PrintngReader printngReader = readerSource.printngReader(request);
        Reader reader = printngReader.reader();
        PrintSpec spec =  new PrintSpec(ParsedDocument.parse(reader));
        PrintSpecFormConfigurator.configure(spec, request.getResourceRef().getQueryAsForm());
        return spec;
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        PrintngWriter writer = getWriter();
        PrintSpec printSpec = getPrintSpec();
        try {
            Logging.getLogger(getClass()).info("printing with " + printSpec);
            writer.write(printSpec, outputStream);
        } catch (PrintSpecException e) {
            throw new RestletException(e.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST, e);
        }
    }

}
