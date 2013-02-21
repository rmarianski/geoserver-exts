package org.opengeo.data.importer.mosaic;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.opengeo.data.importer.DataFormat;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.FileData;
import org.opengeo.data.importer.RasterFormat;
import org.opengeo.data.importer.job.ProgressMonitor;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

public class Mosaic extends Directory {

    public Mosaic(File file) {
        super(file, false);
    }

    @Override
    public void prepare(ProgressMonitor m) throws IOException {
        super.prepare(m);

        //strip away the shapefile index, properties file, and sample_image file
        files.removeAll(Collections2.filter(files, new Predicate<FileData>() {
            @Override
            public boolean apply(FileData input) {
                File f = input.getFile();
                String basename = FilenameUtils.getBaseName(f.getName());

                //is this file part a shapefile or properties file?
                if (new File(f.getParentFile(), basename+".shp").exists() || 
                    new File(f.getParentFile(), basename+".properties").exists()) {
                    return true;
                }

                if ("sample_image".equals(basename)) {
                    return true;
                }

                return false;
            }
        }));

        DataFormat format = format();
        if (format == null) {
            throw new IllegalArgumentException("Unable to determine format for mosaic files");
        }

        if (!(format instanceof RasterFormat)) {
            throw new IllegalArgumentException("Mosaic directory must contain only raster files");
        }

        setFormat(new MosaicFormat());
    }
}
