package org.opengeo.data.importer.web;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.wicket.model.Model;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;
import org.opengeo.data.importer.FileData;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.mosaic.Mosaic;

public class MosaicPanel extends SpatialFilePanel {

    public MosaicPanel(String id) {
        super(id);
    }

    @Override
    protected void initFileChooser(GeoServerFileChooser fileChooser) {
        fileChooser.setFilter(new Model((Serializable)DirectoryFileFilter.DIRECTORY));
    }

    @Override
    public ImportData createImportSource() throws IOException {
        return new Mosaic(new File(this.file));
    }
}
