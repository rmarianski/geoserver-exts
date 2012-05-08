package org.geoserver.uploader;

public class InvalidParameterException extends IllegalArgumentException {
    private static final long serialVersionUID = 8145661470005321165L;

    private String locator;

    public InvalidParameterException(String param) {
        this(param, null, null);
    }

    public InvalidParameterException(String locator, String message) {
        this(locator, message, null);
    }

    public InvalidParameterException(String locator, String message, Throwable cause) {
        super(message, cause);
        this.locator = locator;
    }

    public String getLocator() {
        return locator;
    }
}
