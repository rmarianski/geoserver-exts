package org.opengeo.data.importer;

import java.io.File;
import java.io.IOException;

public class Archive extends Directory {

    public Archive(File file) throws IOException {
        super(Directory.createFromArchive(file).getFile());
    }

}
