package org.geoserver.uploader;

/**
 * Exception signaling that more user input is needed before the upload can be finished.
 * <p>
 * In the event of such an exception being caught, the upload needs to be moved to the staging area
 * until either getting the missing information from the user or the upload expires.
 * </p>
 * 
 * @author groldan
 */
public class MissingInformationException extends Exception {
    private static final long serialVersionUID = -9217705983111190714L;

    private String locator;

    private String uploadToken;

    public MissingInformationException(String locator, String message) {
        super(message);
        this.locator = locator;
    }

    public void setToken(String token) {
        this.uploadToken = token;
    }

    public String getToken() {
        return uploadToken;
    }
    
    public String getLocator(){
        return locator;
    }
}
