package org.opengeo.data.importer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple filter used to constrain the tasks/items process during an import. 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class BasicImportFilter implements ImportFilter {

    Map<ImportTask,List<ImportItem>> map = new HashMap<ImportTask, List<ImportItem>>(); 

    public void add(ImportTask task, List<ImportItem> items) {
        List<ImportItem> l = map.get(task);
        if (l == null) {
            map.put(task, items);
        }
        else {
            l.addAll(items);
        }
    }

    public boolean include(ImportTask task) {
        return map.containsKey(task);
    }

    public boolean include(ImportItem item) {
        return include(item.getTask()) && map.get(item.getTask()).contains(item);
    }
}
