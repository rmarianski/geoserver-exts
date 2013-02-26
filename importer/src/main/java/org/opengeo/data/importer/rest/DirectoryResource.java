package org.opengeo.data.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.FileData;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.Importer;
import org.opengeo.data.importer.SpatialFile;
import org.opengeo.data.importer.mosaic.Granule;
import org.opengeo.data.importer.rest.DataResource.ImportDataJSONFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;

public class DirectoryResource extends BaseResource {

    public DirectoryResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new DirectoryJSONFormat());
    }

    @Override
    public void handleGet() {
        Directory dir = lookupDirectory();
        Object response = dir;

        if (getAttribute("file") != null) {
            response = lookupFile(dir);
        }

        DataFormat formatGet = getFormatGet();
        if (formatGet == null) {
            formatGet = new DirectoryJSONFormat();
        }

        getResponse().setEntity(formatGet.toRepresentation(response));
    }

    @Override
    public boolean allowPut() {
        return getAttribute("file") != null;
    }

    @Override
    public void handlePut() {
        //TODO: this only handles granule timestamps at the moment, expand to handle more
        JSONObject obj = (JSONObject) getFormatPostOrPut().toObject(getRequest().getEntity());
        FileData file = lookupFile();
        if (file instanceof SpatialFile) {
            SpatialFile sf = (SpatialFile) file;
            if (sf instanceof Granule) {
                Granule g = (Granule) sf;
                if (obj.has("timestamp")) {
                    String ts = obj.getString("timestamp");
                    try {
                        g.setTimestamp(ImportJSONIO.DATE_FORMAT.parse(ts));
                    } catch (ParseException e) {
                        throw new RestletException("Could not parse timestamp: " + ts + ", must be " 
                            + "format: " + ImportJSONIO.DATE_FORMAT.toPattern(), 
                            Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                }
            }
        }

        getResponse().setStatus(Status.SUCCESS_OK);
    }

    public boolean allowDelete() {
        return allowPut();
    }

    public void handleDelete() {
        Directory dir = lookupDirectory();
        FileData file = lookupFile();

        if (dir.getFiles().remove(file)) {
            getResponse().setStatus(Status.SUCCESS_OK);
        }
        else {
            throw new RestletException("Unable to remove file: " + file.getName(), 
                Status.CLIENT_ERROR_BAD_REQUEST);
        }

    };

    Directory lookupDirectory() {
        ImportContext context = lookupContext();
        if (!(context.getData() instanceof Directory)) {
            throw new RestletException("Data is not a directory", Status.CLIENT_ERROR_BAD_REQUEST);    
        }

        return (Directory) context.getData();
    }

    FileData lookupFile() {
        return lookupFile(lookupDirectory());
    }

    FileData lookupFile(Directory dir ) {
        final String file = getAttribute("file");
        try {
            if (file != null) {
                return Iterators.find(dir.getFiles().iterator(), new Predicate<FileData>() {
                    @Override
                    public boolean apply(FileData input) {
                        return input.getFile().getName().equals(file);
                    }
                });
            }
        }
        catch(NoSuchElementException e) {
            
        }
        throw new RestletException("No such file: " + file, Status.CLIENT_ERROR_NOT_FOUND);
    }

    class DirectoryJSONFormat extends StreamDataFormat {

        protected DirectoryJSONFormat() {
            super(MediaType.APPLICATION_JSON);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return new ImportJSONIO(importer).parse(in);
            //return new ImportJSONIO(importer).directory(in);
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            ImportJSONIO io = new ImportJSONIO(importer);

            if (object instanceof Directory) {
                Directory d = (Directory) object;
                String path = getRequest().getResourceRef().getPath();
                if (path.matches(".*/files/?")) {
                    io.files(d, out);
                }
                else {
                    io.directory(d, getPageInfo(), out);
                }
            }
            else {
                io.file((FileData)object, getPageInfo(), out);
            }
        }
    }
}
