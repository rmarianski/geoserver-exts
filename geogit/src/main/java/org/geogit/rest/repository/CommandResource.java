/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the GNU GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.rest.repository;

import static org.geogit.rest.repository.GeogitResourceUtils.getGeogit;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.geogit.api.GeoGIT;
import org.geogit.web.api.CommandBuilder;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ParameterSet;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 *
 */
public class CommandResource extends Resource {

    private static final Variant JSON = new Variant(MediaType.APPLICATION_JSON);

    private static final Variant XML = new Variant(MediaType.APPLICATION_XML);

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        List<Variant> variants = getVariants();
        variants.add(XML);
        variants.add(JSON);
    }

    @Override
    public Variant getPreferredVariant() {
        Request request = getRequest();
        String extension = RESTUtils.getAttribute(request, "extension");
        if ("xml".equals(extension)) {
            return XML;
        }
        if ("json".equals(extension)) {
            return JSON;
        }
        return super.getPreferredVariant();
    }

    @Override
    public Representation getRepresentation(Variant variant) {
        Request request = getRequest();
        Representation representation = runCommand(variant, request);
        return representation;
    }

    private Representation runCommand(Variant variant, Request request) {

        final Optional<GeoGIT> geogit = getGeogit(request);
        Preconditions.checkState(geogit.isPresent());

        Representation rep = null;
        WebAPICommand command = null;
        Form options = getRequest().getResourceRef().getQueryAsForm();
        String commandName = (String) getRequest().getAttributes().get("command");
        MediaType format = resolveFormat(options, variant);
        try {
            ParameterSet params = new FormParams(options);
            command = CommandBuilder.build(commandName, params);
            assert command != null;
        } catch (CommandSpecException ex) {
            rep = formatException(ex, format);
        }
        try {
            if (command != null) {
                RestletContext ctx = new RestletContext(geogit.get());
                command.run(ctx);
                rep = ctx.getRepresentation(format, getJSONPCallback());
            }
        } catch (IllegalArgumentException ex) {
            rep = formatException(ex, format);
        } catch (Exception ex) {
            rep = formatUnexpectedException(ex, format);
        }
        return rep;
    }

    private Representation formatException(IllegalArgumentException ex, MediaType format) {
        Logger logger = getLogger();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "CommandSpecException", ex);
        }
        return new JettisonRepresentation(format, CommandResponse.error(ex.getMessage()),
                getJSONPCallback());
    }

    private Representation formatUnexpectedException(Exception ex, MediaType format) {
        Logger logger = getLogger();
        UUID uuid = UUID.randomUUID();
        String stack = "";
        StackTraceElement[] trace = ex.getStackTrace();
        for (int index = 0; index < 5; index++) {
            if (index < trace.length) {
                stack += trace[index].toString() + '\t';
            } else {
                break;
            }
        }
        logger.log(Level.SEVERE, "Unexpected exception : " + uuid, ex);
        return new JettisonRepresentation(format, CommandResponse.error(stack), getJSONPCallback());
    }

    private String getJSONPCallback() {
        Form form = getRequest().getResourceRef().getQueryAsForm();
        return form.getFirstValue("callback", null);
    }

    private MediaType resolveFormat(Form options, Variant variant) {
        MediaType retval = variant.getMediaType();
        String requested = options.getFirstValue("output_format");
        if (requested != null) {
            if (requested.equalsIgnoreCase("xml")) {
                retval = MediaType.APPLICATION_XML;
            } else if (requested.equalsIgnoreCase("json")) {
                retval = MediaType.APPLICATION_JSON;
            } else {
                throw new RestletException("Invalid output_format '" + requested + "'",
                        org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }
        return retval;
    }

    static class RestletContext implements CommandContext {

        CommandResponse responseContent;

        final GeoGIT geogit;

        RestletContext(GeoGIT geogit) {
            this.geogit = geogit;
        }

        @Override
        public GeoGIT getGeoGIT() {
            return geogit;
        }

        Representation getRepresentation(MediaType format, String callback) {
            return new JettisonRepresentation(format, responseContent, callback);
        }

        @Override
        public void setResponseContent(CommandResponse responseContent) {
            this.responseContent = responseContent;
        }
    }

    static class JettisonRepresentation extends WriterRepresentation {

        final CommandResponse impl;

        String callback;

        public JettisonRepresentation(MediaType mediaType, CommandResponse impl, String callback) {
            super(mediaType);
            this.impl = impl;
            this.callback = callback;
        }

        private XMLStreamWriter createWriter(Writer writer) {
            final MediaType mediaType = getMediaType();
            XMLStreamWriter xml;
            if (mediaType.getSubType().equalsIgnoreCase("xml")) {
                try {
                    xml = XMLOutputFactory.newFactory().createXMLStreamWriter(writer);
                } catch (XMLStreamException ex) {
                    throw new RuntimeException(ex);
                }
                callback = null; // this doesn't make sense
            } else if (mediaType == MediaType.APPLICATION_JSON) {
                xml = new MappedXMLStreamWriter(new MappedNamespaceConvention(), writer);
            } else {
                throw new RuntimeException("mediatype not handled " + mediaType);
            }
            return xml;
        }

        @Override
        public void write(Writer writer) throws IOException {
            XMLStreamWriter stax = null;
            if (callback != null) {
                writer.write(callback);
                writer.write('(');
            }
            try {
                stax = createWriter(writer);
                impl.write(new ResponseWriter(stax));
                stax.flush();
                stax.close();
            } catch (Exception ex) {
                throw new IOException(ex);
            }
            if (callback != null) {
                writer.write(");");
            }
        }
    }
}
