package org.opengeo.mapmeter.monitor.saas;

public class MissingMapmeterApiKeyException extends Exception {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public MissingMapmeterApiKeyException(String message) {
        super(message);
    }

}
