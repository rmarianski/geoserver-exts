package org.opengeo.data.importer.mosaic;

import java.io.IOException;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.opengeo.data.importer.GridFormat;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.job.ProgressMonitor;

public class MosaicFormat extends GridFormat {

    public MosaicFormat() {
        super(ImageMosaicFormat.class);
    }

    @Override
    public List<ImportItem> list(ImportData data, Catalog catalog, ProgressMonitor monitor) 
            throws IOException {
        return super.list(data, catalog, monitor);
    }
}
