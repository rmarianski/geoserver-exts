package org.opengeo.data.importer.web;

import org.apache.wicket.model.LoadableDetachableModel;
import org.opengeo.data.importer.ImportItem;

public class ImportItemModel extends LoadableDetachableModel<ImportItem> {

    long context;
    long task;
    long id;

    public ImportItemModel(ImportItem item) {
        this(item.getTask().getContext().getId(), item.getTask().getId(), item.getId());
    }

    public ImportItemModel(long context, long task, long id) {
        this.context = context;
        this.task = task;
        this.id = id;
    }

    @Override
    protected ImportItem load() {
        return ImporterWebUtils.importer().getContext(context).task(task).item(id);
    }

}
