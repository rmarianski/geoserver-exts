package org.geoserver.uploader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.opengis.coverage.grid.Format;

class CoverageUploader extends LayerUploader {

    private static final List<String> supportedExtensions = Arrays.asList(".tif", ".tiff");

    protected static Map<String, String> formatToCoverageStoreFormat = new HashMap<String, String>();
    static {
        for (Format format : CoverageStoreUtils.formats) {
            formatToCoverageStoreFormat.put(format.getName().toLowerCase(), format.getName());
        }
    }

    public CoverageUploader(Catalog catalog, final WorkspaceInfo targetWorkspace) {
        super(catalog, targetWorkspace);
    }

    public static boolean canHandle(File spatialFile) {
        String extension = VFSWorker.getExtension(spatialFile.getName().toLowerCase());
        boolean canHandle = supportedExtensions.contains(extension);
        return canHandle;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public LayerInfo importFromFile(File file) throws InvalidParameterException, RuntimeException {
        file = ensureUnique(workspaceInfo, file);

        Map<String, String> formatToCoverageStoreFormat = CoverageUploader.formatToCoverageStoreFormat;

        String coverageFormatName = formatToCoverageStoreFormat.get("geotiff");
        Format coverageFormat = null;
        try {
            coverageFormat = CoverageStoreUtils.acquireFormat(coverageFormatName);
        } catch (Exception e) {
            throw new RuntimeException("Error acquiring reader for provided raster file", e);
        }
        final String coverageName = FilenameUtils.getBaseName(file.getName());
        URL coverageURL;
        try {
            coverageURL = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace(super.workspaceInfo);

        CoverageStoreInfo storeInfo = builder.buildCoverageStore(coverageName);
        storeInfo.setDescription(_abstract);
        storeInfo.setType(coverageFormat.getName());
        storeInfo.setURL(coverageURL.toExternalForm());
        builder.setStore(storeInfo);

        AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) ((AbstractGridFormat) coverageFormat)
                .getReader(coverageURL);
        if (reader == null) {
            throw new RuntimeException("Error acquiring reader for provided raster file");
        }

        // coverage read params
        final Map customParameters = new HashMap();
        Boolean useJAIImageReadParam = Boolean.TRUE;
        if (useJAIImageReadParam != null) {
            customParameters.put(AbstractGridFormat.USE_JAI_IMAGEREAD.getName().toString(),
                    useJAIImageReadParam);
        }

        CoverageInfo coverageInfo;
        try {
            coverageInfo = builder.buildCoverage(reader, customParameters);
        } catch (Exception e) {
            throw new RuntimeException("Error building coverage information", e);
        }
        coverageInfo
                .setTitle(title == null || title.length() == 0 ? coverageInfo.getName() : title);
        coverageInfo.setAbstract(_abstract);

        // do some post configuration, if srs is not known or unset, transform to 4326
        if ("UNKNOWN".equals(coverageInfo.getSRS())) {
            // CoordinateReferenceSystem sourceCRS =
            // cinfo.getBoundingBox().getCoordinateReferenceSystem();
            // CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true);
            // ReferencedEnvelope re = cinfo.getBoundingBox().transform(targetCRS, true);
            coverageInfo.setSRS("EPSG:4326");
            // cinfo.setCRS( targetCRS );
            // cinfo.setBoundingBox( re );
        }

        LayerInfo layerInfo;
        try {
            layerInfo = builder.buildLayer(coverageInfo);
        } catch (IOException e) {
            throw new RuntimeException("Error building coverage information", e);
        }
        try {
            catalog.add(storeInfo);
            catalog.add(coverageInfo);
            catalog.add(layerInfo);
        } catch (RuntimeException e) {
            try {
                catalog.remove(layerInfo);
            } finally {
            }
            try {
                catalog.remove(coverageInfo);
            } finally {
            }
            try {
                catalog.remove(storeInfo);
            } finally {
            }
            throw new RuntimeException("Error registering coverage", e);
        }
        return layerInfo;
    }

}
