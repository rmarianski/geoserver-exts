package org.geoserver.printng.resource;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.geoserver.printng.io.PrintngReader;
import org.geoserver.printng.io.PrintngReaderFactory;
import org.geoserver.printng.io.PrintngWriter;
import org.geoserver.printng.io.PrintngWriterFactory;
import org.geoserver.rest.RestletException;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;
import org.w3c.dom.Document;

public class PrintResource extends Resource {
    private final PrintngReaderFactory readerFactory;

    private final PrintngWriterFactory writerFactory;

    private final Variant variant;

    public PrintResource(Request request, Response response, Variant variant,
            PrintngReaderFactory readerFactory, PrintngWriterFactory writerFactory) {
        super(null, request, response);
        this.variant = variant;
        this.readerFactory = readerFactory;
        this.writerFactory = writerFactory;
    }

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        List<Variant> allVariants = getVariants();
        allVariants.add(variant);
    }

    @Override
    public Representation getRepresentation(Variant variant) {
        PrintngReader printngReader = readerFactory.printngReader();
        Document document;
        try {
            Reader reader = printngReader.reader();
            // TODO check if this exists already or if we create this class
            PrintngRestDocumentParser documentParser = new PrintngRestDocumentParser(reader);
            document = documentParser.parse();
        } catch (IOException e) {
            throw new RestletException("Error reading input", Status.SERVER_ERROR_INTERNAL, e);
        }
        PrintngWriter writer = writerFactory.printngWriter();
        return new PrintRepresentation(variant.getMediaType(), document, writer);
    }
}
