package org.opengeo.analytics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.opengeo.analytics.Service;

/**
 * Maintains state of selected services.
 */
public class ServiceSelection implements Serializable {
    private Set<Service> selected = new HashSet(Arrays.asList(Service.values()));
    private boolean showFailed = false;

    public void setWms(boolean selected) {
        set(Service.WMS, selected);
    }

    public boolean isWms() {
        return isSet(Service.WMS);
    }

    public void setWfs(boolean selected) {
        set(Service.WFS, selected);
    }

    public boolean isWfs() {
        return isSet(Service.WFS);
    }

    public void setWcs(boolean selected) {
        set(Service.WCS, selected);
    }

    public boolean isWcs() {
        return isSet(Service.WCS);
    }

    public void setOther(boolean selected) {
        set(Service.OTHER, selected);
    }

    public boolean isOther() {
        return isSet(Service.OTHER);
    }

    public void setShowFailed(boolean selected) {
        showFailed = selected;
    }

    public boolean isShowFailed() {
        return showFailed;
    }

    public boolean isSet(Service s) {
        return selected.contains(s);
    }

    public void set(Service s, boolean set) {
        if (set) {
            selected.add(s);
        } else {
            selected.remove(s);
        }
    }

    public Set<Service> getSelected() {
        return selected;
    }

    public Set<String> getSelectedAsString() {
        Set<String> set = new HashSet();
        for (Service s : selected) {
            set.add(s.name());
        }
        return set;
    }
    
}
