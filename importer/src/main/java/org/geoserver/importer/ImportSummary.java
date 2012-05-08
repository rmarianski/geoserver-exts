/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.geoserver.catalog.LayerInfo;

/**
 * Contains summary information about the whole import process
 */
@SuppressWarnings("serial")
public class ImportSummary implements Serializable {
    long startTime;

    long endTime;

    int totalLayers;

    int processedLayers;

    int failures;

    String currentLayer;

    Exception error;

    String project;

    boolean workspaceNew;

    boolean storeNew;

    boolean done;

    // concurrent list so that we can manipulate it while it's being iterated over
    List<LayerSummary> layers = new CopyOnWriteArrayList<LayerSummary>();

    public ImportSummary(String project, boolean workspaceNew, boolean storeNew) {
        this.project = project;
        this.startTime = System.currentTimeMillis();
        this.workspaceNew = workspaceNew;
        this.storeNew = storeNew;
    }

    void setTotalLayers(int totalLayers) {
        this.totalLayers = totalLayers;
    }

    public String getProject() {
        return project;
    }

    public void newLayer(String currentLayer) {
        this.currentLayer = currentLayer;
    }

    void end(Exception error) {
        this.error = error;
        this.currentLayer = null;
        this.endTime = System.currentTimeMillis();
    }

    void end() {
        this.done = true;
        this.currentLayer = null;
        this.endTime = System.currentTimeMillis();
    }

    public boolean isCompleted() {
        return done;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getTotalLayers() {
        return totalLayers;
    }

    public List<LayerSummary> getLayers() {
        return layers;
    }

    public int getProcessedLayers() {
        return processedLayers;
    }
    
    public int getFailures() {
        return failures;
    }

    public String getCurrentLayer() {
        return currentLayer;
    }

    void completeLayer(String layerName, LayerInfo layer, ImportStatus status) {
        layers.add(new LayerSummary(layerName, layer, status));
        processedLayers++;
        if (!status.successful())
            failures++;
    }

    void completeLayer(String layerName, LayerInfo layer, Exception error) {
        layers.add(new LayerSummary(layerName, layer, error));
        processedLayers++;
        failures++;
    }

    public boolean isWorkspaceNew() {
        return workspaceNew;
    }

    public boolean isStoreNew() {
        return storeNew;
    }

    public Exception getError() {
        return error;
    }

}
