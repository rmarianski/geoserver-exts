package org.opengeo.data.importer.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.ImportTask;

public class ImportItemProvider extends GeoServerDataProvider<ImportItem> {

    public static Property<ImportItem> NAME = new BeanProperty("name", "layer.name");
    //public static Property<ImportItem> FORMAT = new BeanProperty("format", "format");
    public static Property<ImportItem> STATUS = new BeanProperty("status", "state");
    public static Property<ImportItem> ACTION = new BeanProperty("action", "state");

    IModel<ImportTask> task;

    public ImportItemProvider(ImportTask task) {
        this(new ImportTaskModel(task));
    }

    public ImportItemProvider(IModel<ImportTask> task) {
        this.task = task;
    }

    @Override
    protected List<Property<ImportItem>> getProperties() {
        return Arrays.asList(NAME, STATUS, ACTION);
    }

    @Override
    protected List<ImportItem> getItems() {
        List<ImportItem> items = new ArrayList(task.getObject().getItems());
//        Collections.sort(items, new Comparator<ImportItem>() {
//            public int compare(ImportItem o1, ImportItem o2) {
//                return o1.getState().compareTo(o2.getState());
//            }
//        });
        return items;
    }

    @Override
    protected IModel newModel(Object object) {
        return new ImportItemModel((ImportItem)object);
    }
}
