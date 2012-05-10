package org.opengeo.analytics;

public enum Service {

    WMS("#1751a7"),
    WFS("#8aa717"),
    WCS("#a74217"),
    OTHER("#a78a17") {
        @Override
        public String displayName() {
            return "Other";
        }
    };
    
    private Service(String color) {
        this.color = color;
    }
    
    private final String color;
    
    public String color() {
        return color;
    }
    
    public String displayName() {
        return name();
    }
}
