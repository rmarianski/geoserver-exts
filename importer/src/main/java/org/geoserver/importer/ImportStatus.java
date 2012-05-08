/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

/**
 * The result of the import process for a certain layer
 */
public enum ImportStatus {
    SUCCESS(true), DEFAULTED_SRS(true), DUPLICATE(false), MISSING_NATIVE_CRS(false), NO_SRS_MATCH(false), MISSING_BBOX(false), OTHER(false);
    
    boolean success;
    ImportStatus(boolean success) {
        this.success = success;
    }
    
    public boolean successful() {
        return success;
    }
}
