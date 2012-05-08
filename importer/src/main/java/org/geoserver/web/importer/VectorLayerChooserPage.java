/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.util.Set;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.importer.FeatureTypeImporter;
import org.geoserver.importer.ImporterThreadManager;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.opengis.feature.type.Name;

/**
 * Allows to choose the vector layers that need to be imported
 * 
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class VectorLayerChooserPage extends GeoServerSecuredPage {

    GeoServerTablePanel<Resource> layers;

    boolean workspaceNew;

    boolean storeNew;

    private String wsName;

    private String storeName;

    public VectorLayerChooserPage(PageParameters params) {
        wsName = params.getString("workspace");
        storeName = params.getString("store");
        storeNew = params.getBoolean("storeNew");
        workspaceNew = params.getBoolean("workspaceNew");
        boolean skipGeometryless = params.getBoolean("skipGeometryless");

        // if we don't find the store for any reason go back to the first page
        StoreInfo store = getCatalog().getDataStoreByName(wsName, storeName);
        if (store == null) {
            error(new ParamResourceModel("storeNotFound", this, storeName, wsName).getString());
        }

        // check if we have anything to import
        LayerChooserProvider provider = new LayerChooserProvider(store.getId(), skipGeometryless);
        if (provider.size() <= 0) {
            error(new ParamResourceModel("storeEmpty", this, storeName, wsName).getString());
        }

        // build the GUI
        Form form = new Form("form", new CompoundPropertyModel(this));
        add(form);
        layers = new GeoServerTablePanel<Resource>("layerChooser", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<Resource> property) {
                Resource resource = (Resource) itemModel.getObject();
                if (property == LayerChooserProvider.TYPE) {
                    return new Icon(id, resource.getIcon());
                } else if (property == LayerChooserProvider.NAME) {
                    return new Label(id, property.getModel(itemModel));
                }
                return null;
            }

        };
        layers.setFilterable(false);
        layers.selectAll();
        form.add(layers);

        // submit
        SubmitLink submitLink = submitLink();
        form.add(submitLink);
        form.setDefaultButton(submitLink);
    }

    SubmitLink submitLink() {
        return new SubmitLink("import") {

            @Override
            public void onSubmit() {
                try {
                    // grab the selection
                    Set<Name> names = new java.util.HashSet<Name>();
                    for (Resource r : layers.getSelection()) {
                        names.add(r.getName());
                    }

                    // if nothing was selected we need to go back
                    if (names.size() == 0) {
                        error(new ParamResourceModel("selectionEmpty", VectorLayerChooserPage.this)
                                .getString());
                        return;
                    }

                    DataStoreInfo store = getCatalog().getDataStoreByName(wsName, storeName);

                    // build and run the importer
                    FeatureTypeImporter importer = new FeatureTypeImporter(store, null, names,
                            getCatalog(), workspaceNew, storeNew);
                    ImporterThreadManager manager = (ImporterThreadManager) getGeoServerApplication()
                            .getBean("importerPool");
                    String importerKey = manager.startImporter(importer);

                    setResponsePage(new ImportProgressPage(importerKey));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while setting up mass import", e);
                }

            }
        };
    }

}
