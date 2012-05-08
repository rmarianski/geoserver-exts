package org.opengeo.data.importer.web;

import java.io.Serializable;

import org.apache.wicket.model.Model;
import org.opengeo.data.importer.ImportContext;

public class ImportTasksModel extends Model {

    ImportContext imp;

    public ImportTasksModel(ImportContext imp) {
        super((Serializable) imp.getTasks());
    }

}
