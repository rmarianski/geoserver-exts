package org.geoserver.uploader;

import org.springframework.scheduling.concurrent.ScheduledExecutorTask;

/**
 * A {@link Runnable} meant to be set to a {@link ScheduledExecutorTask} in order to periodically
 * clean up the pending uploads (from the {@code <data dir>/uploader/pending} directory)
 * <p>
 * There's a single instance of this class that's called at each {@link ScheduledExecutorTask}
 * invocation.
 * </p>
 * 
 * @author groldan
 * @see UploadLifeCyleManager#cleanUpOldPendingUploads(int)
 */
public class PeriodicPendingUploadsCleanerTask implements Runnable {

    private final UploadLifeCyleManager uploadLifeCycleManager;

    private final int maxPendingUploadTimeSeconds;

    /**
     * @param who
     *            's gonna actually clean up the pending uploads
     * @param pendingUploadTimeoutSecs
     *            max age a pending upload can be before being wiped out completely
     */
    public PeriodicPendingUploadsCleanerTask(UploadLifeCyleManager cleaner,
            int pendingUploadTimeoutSecs) {
        this.uploadLifeCycleManager = cleaner;
        this.maxPendingUploadTimeSeconds = pendingUploadTimeoutSecs;
    }

    public void run() {
        uploadLifeCycleManager.cleanUpOldPendingUploads(maxPendingUploadTimeSeconds);
    }

}
