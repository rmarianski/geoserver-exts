package org.geoserver.uploader;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;

abstract class LayerUploader {

    protected Catalog catalog;

    protected String title;

    protected String _abstract;

    protected WorkspaceInfo workspaceInfo;

    public LayerUploader(Catalog catalog, WorkspaceInfo targetWorkspace) {
        this.catalog = catalog;
        this.workspaceInfo = targetWorkspace;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAbstract(String _abstract) {
        this._abstract = _abstract == null ? "" : _abstract;
    }

    public abstract LayerInfo importFromFile(File file) throws MissingInformationException;

    protected File ensureUnique(final WorkspaceInfo workspaceInfo, final File resourceFile) {
        final NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspaceInfo.getName());
        final String originalResourceName = FilenameUtils.getBaseName(resourceFile.getName());

        String resourceName = originalResourceName;
        int tries = 0;
        while (null != catalog.getResourceByName(namespace, resourceName, ResourceInfo.class)
                && tries < 100) {
            tries++;
            resourceName = originalResourceName + "_" + tries;
        }
        File uniqueResourceFile = resourceFile;
        if (!originalResourceName.equals(resourceName)) {
            uniqueResourceFile = renameTo(resourceFile, resourceName);
        }
        return uniqueResourceFile;
    }

    protected File renameTo(final File file, final String newNameNoExtension) {
        final VFSWorker vfs = new VFSWorker();

        final File directory = file.getParentFile();

        final String renameMatching = FilenameUtils.getBaseName(file.getName()) + ".";
        final File[] renameFiles = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (!directory.equals(dir)) {
                    return false;
                }
                if (name.startsWith(renameMatching)) {
                    return true;
                }
                return false;
            }
        });

        File renamed = null;
        for (File renameFrom : renameFiles) {
            String extension = vfs.getExtension(renameFrom.getName());
            String newName = newNameNoExtension + extension;
            File renameTo = new File(directory, newName);
            renameFrom.renameTo(renameTo);
            if (file.getName().equals(renameFrom.getName())) {
                renamed = renameTo;
            }
        }

        return renamed;
    }

}
