package org.opengeo.data.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.csvreader.CsvReader;

public class CSVFileState {

    private final File file;

    private final String typeName;

    private final CoordinateReferenceSystem crs;

    private final URI namespace;

    private final String dataInput;

    private volatile String[] headers = null;

    public CSVFileState(File file, String typeName, CoordinateReferenceSystem crs, URI namespace) {
        this.file = file;
        this.typeName = typeName;
        this.crs = crs;
        this.namespace = namespace;
        this.dataInput = null;
    }

    // used by unit tests
    public CSVFileState(String dataInput, String typeName, CoordinateReferenceSystem crs,
            URI namespace) {
        this.dataInput = dataInput;
        this.typeName = typeName;
        this.crs = crs;
        this.namespace = namespace;
        this.file = null;
    }

    public CSVFileState(String dataInput, String typeName, CoordinateReferenceSystem crs) {
        this(dataInput, typeName, crs, null);
    }

    public URI getNamespace() {
        return namespace;
    }

    public File getFile() {
        return file;
    }

    public String getTypeName() {
        return typeName;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public CsvReader openCSVReader() throws IOException {
        Reader reader;
        if (file != null) {
            reader = new BufferedReader(new FileReader(file));
        } else {
            reader = new StringReader(dataInput);
        }
        CsvReader csvReader = new CsvReader(reader);
        if (!csvReader.readHeaders()) {
            throw new IOException("Error reading csv headers");
        }
        return csvReader;
    }

    public String[] getCSVHeaders() {
        if (headers == null) {
            synchronized (this) {
                if (headers == null) {
                    headers = readCSVHeaders();
                }
            }
        }
        return headers;
    }

    private String[] readCSVHeaders() {
        CsvReader csvReader = null;
        try {
            csvReader = openCSVReader();
            return csvReader.getHeaders();
        } catch (IOException e) {
            throw new RuntimeException("Failure reading csv headers", e);
        } finally {
            if (csvReader != null) {
                csvReader.close();
            }
        }
    }
}
