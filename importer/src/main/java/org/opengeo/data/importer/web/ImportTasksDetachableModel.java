package org.opengeo.data.importer.web;

import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportTask;

public class ImportTasksDetachableModel extends LoadableDetachableModel<List<ImportTask>> {

    long id;

    public ImportTasksDetachableModel(ImportContext imp) {
        this(imp.getId());
    }

    public ImportTasksDetachableModel(long id) {
        this.id = id;
    }

    @Override
    protected List<ImportTask> load() {
        return ImporterWebUtils.importer().getContext(id).getTasks();
    }
}
