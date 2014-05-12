package org.opengeo.mapmeter.monitor.saas;

public class MapmeterSaasCredentials {

    private final String username;

    private final String password;

    public MapmeterSaasCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
