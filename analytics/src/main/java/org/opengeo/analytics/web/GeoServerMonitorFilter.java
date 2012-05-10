package org.opengeo.analytics.web;

import org.geoserver.platform.ExtensionFilter;

public class GeoServerMonitorFilter implements ExtensionFilter {

    public boolean exclude(String beanId, Object bean) {
        if ("monitorCategory".equals(beanId)) {
            return true;
        }
        if ("monitorActivityPage".equals(beanId) || "monitorReportPage".equals(beanId)) {
            return true;
        }
        return false;
    }

}
