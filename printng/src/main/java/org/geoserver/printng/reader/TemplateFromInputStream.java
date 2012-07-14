package org.geoserver.printng.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.geoserver.printng.iface.PrintngReader;

public class TemplateFromInputStream implements PrintngReader {

    private final InputStream inputStream;

    public TemplateFromInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public Reader reader() throws IOException {
        return new InputStreamReader(inputStream);
    }

}
