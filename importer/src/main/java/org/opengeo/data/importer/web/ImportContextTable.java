package org.opengeo.data.importer.web;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.opengeo.data.importer.ImportContext;

public class ImportContextTable extends GeoServerTablePanel<ImportContext> {

    public ImportContextTable(String id, ImportContextProvider dataProvider) {
        super(id, dataProvider);
    }

    @Override
    protected Component getComponentForProperty(String id, IModel itemModel, Property property) {
        if (ImportContextProvider.ID == property) {
            PageParameters pp = new PageParameters();
            pp.put("id", property.getModel(itemModel).getObject());
            return new SimpleBookmarkableLink(id, ImportPage.class, property.getModel(itemModel), pp);
        }
        return new Label(id, property.getModel(itemModel));
    }

}
