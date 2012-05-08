package org.geoserver.uploader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.util.logging.Logging;

public class UploadLifeCyleManager {

    private static final Logger LOGGER = Logging.getLogger(UploadLifeCyleManager.class);

    private GeoServerDataDirectory dataDir;

    private final Lock lock = new ReentrantLock();

    public UploadLifeCyleManager(GeoServerDataDirectory dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * Moves {@code uploadDirectory} to the pendings directory and returns a unique token for it.
     */
    public String saveAsPending(final File uploadDirectory) {

        final String token = String.valueOf(System.currentTimeMillis());

        lock.lock();
        try {
            final File newPendingDir = getPendingUploadDir(token);

            if (!newPendingDir.mkdirs()) {
                throw new RuntimeException("Can't create pending directory "
                        + newPendingDir.getParentFile().getName() + File.separator
                        + newPendingDir.getName());
            }
            final File destination = new File(newPendingDir, uploadDirectory.getName());
            /*
             * fast path
             */
            final boolean renamed = uploadDirectory.renameTo(destination);
            /*
             * May the data dir be on a different filesystem or something else that prevents the
             * renaming? try the slow option
             */
            if (!renamed) {
                try {
                    FileUtils.copyDirectory(uploadDirectory, destination);
                } catch (IOException e) {
                    throw new RuntimeException("Can't move " + uploadDirectory.getName()
                            + " to pending directory ", e);
                }
            }
            return token;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the pending directory for the given {@code token}; it may not exist
     */
    public File getPendingUploadDir(final String token) {
        final File pendingRoot = getPendingUploadRoot();
        final File newPendingDir = new File(pendingRoot, token);
        return newPendingDir;
    }

    /**
     * Whips out pending uploads older then {@code maxPendingUploadTimeSeconds}
     * <p>
     * This method relies on the pending upload subdirectories to be named after its creation
     * timestamp in seconds. This is because some operating systems do not remember the file
     * creation date.
     * <p>
     */
    public boolean cleanUpOldPendingUploads(int maxPendingUploadTimeSeconds) {

        final File pendingRoot = getPendingUploadRoot();

        final long currentTimeSecs = System.currentTimeMillis() / 1000;

        lock.lock();
        try {
            File[] pendings = pendingRoot.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
            for (File pending : pendings) {
                long timestamp;
                try {
                    timestamp = Long.valueOf(pending.getName()) / 1000;
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Unknown directory type under uploader pendings: "
                            + pending.getName());
                    continue;
                }
                final long timeElapsed = currentTimeSecs - timestamp;
                final boolean remove = timeElapsed > maxPendingUploadTimeSeconds || timeElapsed < 0;
                if (timeElapsed < 0) {
                    LOGGER.log(Level.WARNING,
                            "Something weird may be going on with the system's clock."
                                    + " Pending directory with timestamp "
                                    + new Date(timestamp * 1000)
                                    + " is newer than current system time: "
                                    + new Date(currentTimeSecs * 1000));
                }
                if (remove) {
                    try {
                        FileUtils.deleteDirectory(pending);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Error trying to delete old pending upload '"
                                + pending.getAbsolutePath() + "'", e);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        return true;
    }

    public File createTargetDirectory(final String fileItemName) {
        File incomingDirectory = getUploadRoot();
        final File targetDirectory = ensureUniqueDirectory(incomingDirectory,
                FilenameUtils.getBaseName(fileItemName));
        targetDirectory.mkdirs();
        return targetDirectory;
    }

    public void deleteTargetDirectory(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Can't delete directory for uploaded files " + directory.getAbsolutePath(), e);
        }
    }

    private File ensureUniqueDirectory(final File baseDirectory, final String temptativeName) {
        lock.lock();
        try {
            String uniqueName = temptativeName;
            int tries = 0;
            while (new File(baseDirectory, uniqueName).exists()) {
                tries++;
                uniqueName = temptativeName + "_" + tries;
            }
            return new File(baseDirectory, uniqueName);
        } finally {
            lock.unlock();
        }
    }

    private File getUploadRoot() {
        File incoming;
        try {
            incoming = dataDir.findOrCreateDataDir("data", "incoming");
        } catch (IOException e) {
            throw new RuntimeException("Can't create target directory for uploaded data", e);
        }
        return incoming;
    }

    private File getPendingUploadRoot() {
        File pending = new File(new File(dataDir.root(), "uploader"), "pending");
        if (!pending.exists() && !pending.mkdirs()) {
            throw new RuntimeException("Coudn't create uploads pending directory '"
                    + pending.getAbsolutePath() + "'");
        }
        return pending;
    }

    private File getStagingUploadRoot() {
        File pending = new File(new File(dataDir.root(), "uploader"), "staging");
        if (!pending.exists() && !pending.mkdirs()) {
            throw new RuntimeException("Coudn't create uploads pending directory '"
                    + pending.getAbsolutePath() + "'");
        }
        return pending;
    }

    /**
     * @param pendingUploadToken
     * @return a safe directory (like in it won't be deleted by a subsequent call to
     *         {@link #cleanUpOldPendingUploads(int)}) where to recover the pending upload given by
     *         {@code pendingUploadToken}, or {@code null} if no pending upload exists for the given
     *         token.
     */
    public File startRecovery(final String pendingUploadToken) {
        lock.lock();
        try {
            File pendingUploadDir = getPendingUploadDir(pendingUploadToken);
            if (!pendingUploadDir.exists()) {
                return null;
            }
            File stagingUploadRoot = getStagingUploadRoot();
            File newWorkingDir = new File(stagingUploadRoot, pendingUploadDir.getName());
            if (!pendingUploadDir.renameTo(newWorkingDir)) {
                throw new RuntimeException("Can't move pending upload '" + pendingUploadToken
                        + "' to staging area");
            }
            return newWorkingDir;
        } finally {
            lock.unlock();
        }
    }

}
