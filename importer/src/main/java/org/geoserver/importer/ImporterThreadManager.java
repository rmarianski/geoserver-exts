/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Manages importer threads with a pool
 * TODO: forcefully cleanup tasks if the UI did not do so within a certain
 * time of their conclusion
 */
public class ImporterThreadManager  implements ApplicationListener {
    ExecutorService pool = Executors.newCachedThreadPool();
    Map<String, FeatureTypeImporter> tasks = new HashMap<String, FeatureTypeImporter>();
    
    public String startImporter(FeatureTypeImporter importer) {
        String id = new UID().toString();
        tasks.put(id, importer);
        pool.execute(importer);
        return id;
    }
    
    public FeatureTypeImporter getImporter(String id) {
        return tasks.get(id);
    }
    
    public void cleanImporter(String id) {
        tasks.remove(id);
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ContextClosedEvent) {
            pool.shutdown();
        }
    }
    

}
