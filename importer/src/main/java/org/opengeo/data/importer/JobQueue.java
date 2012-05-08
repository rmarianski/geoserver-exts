package org.opengeo.data.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JobQueue {

    /** job id counter */
    AtomicLong counter = new AtomicLong();

    /** recent jobs */
    ConcurrentHashMap<Long,Future<?>> jobs = new ConcurrentHashMap<Long, Future<?>>();

    /** job runner */
    ExecutorService pool = Executors.newCachedThreadPool();

    /** job cleaner */
    ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
    {
        cleaner.scheduleAtFixedRate(new Runnable() {
            public void run() {
                List<Long> toremove = new ArrayList<Long>();
                for (Map.Entry<Long, Future<?>> e : jobs.entrySet()) {
                    if (e.getValue().isDone()) {
                        toremove.add(e.getKey());
                    }
                }
                for (Long l : toremove) {
                    jobs.remove(l);
                }
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    public Long submit(Callable<?> task) {
        Long jobid = counter.getAndIncrement();
        jobs.put(jobid, pool.submit(task));
        return jobid;
    }

    public Future<?> getFuture(Long jobid) {
        return jobs.get(jobid);
    }

    public void shutdown() {
        cleaner.shutdownNow();
        pool.shutdownNow();
    }
}
