/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.importer;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;

/**
 * Sets up the import process and starts it up delegating the progress to {@link ImportProgressPage}
 */
@SuppressWarnings("serial")
public class DirectoryPage extends GeoServerSecuredPage {
    String directory = "";

    GeoServerDialog dialog;

    private TextField dirField;

    private Form form;

    GeneralStoreParamPanel generalParams;

    public DirectoryPage() {
        add(dialog = new GeoServerDialog("dialog"));

        form = new Form("form", new CompoundPropertyModel(this));
        add(form);

        form.add(generalParams = new GeneralStoreParamPanel("generalParams"));
        
        
        dirField = new TextField("directory");
        dirField.add(new DirectoryValidator());
        dirField.setRequired(true);
        dirField.setOutputMarkupId(true);
        form.add(dirField);
        form.add(chooserButton(form));
        

        SubmitLink submitLink = submitLink();
        form.add(submitLink);

        form.setDefaultButton(submitLink);
    }

    private Component chooserButton(Form form) {
        AjaxSubmitLink link = new AjaxSubmitLink("chooser") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                dialog.setTitle(new ParamResourceModel("chooseDirectory", this));
                dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {

                    @Override
                    protected Component getContents(String id) {
                        // use what the user currently typed
                        File file = null;
                        if (!dirField.getInput().trim().equals("")) {
                            file = new File(dirField.getInput());
                            if (!file.exists())
                                file = null;
                        }

                        GeoServerFileChooser chooser = new GeoServerFileChooser(id, new Model(file));
                        chooser.setFilter(new Model(new ExtensionFileFilter(".shp")));
                        return chooser;
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        GeoServerFileChooser chooser = (GeoServerFileChooser) contents;
                        directory = ((File) chooser.getDefaultModelObject()).getAbsolutePath();
                        // clear the raw input of the field won't show the new model value
                        dirField.clearInput();

                        target.addComponent(dirField);
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        // update the field with the user chosen value
                        target.addComponent(dirField);
                    }

                });

            }

        };
        // otherwise the link won't trigger when the form contents are not valid
        link.setDefaultFormProcessing(false);
        return link;
    }

    SubmitLink submitLink() {
        return new SubmitLink("next") {

            @Override
            public void onSubmit() {
                try {
                    // check there is not another store with the same name
                    WorkspaceInfo workspace = generalParams.getWorkpace();
                    NamespaceInfo namespace = getCatalog()
                            .getNamespaceByPrefix(workspace.getName());
                    StoreInfo oldStore = getCatalog().getStoreByName(workspace, generalParams.name,
                            StoreInfo.class);
                    if (oldStore != null) {
                        error(new ParamResourceModel("ImporterError.duplicateStore",
                                DirectoryPage.this, generalParams.name, workspace.getName()).getString());
                        return;
                    }

                    // build/reuse the store
                    String storeType = new ShapefileDataStoreFactory().getDisplayName();
                    Map<String, Serializable> params = new HashMap<String, Serializable>();
                    params.put(ShapefileDataStoreFactory.URLP.key, new File(directory).toURI()
                            .toURL().toString());
                    params.put(ShapefileDataStoreFactory.NAMESPACEP.key, new URI(namespace.getURI()).toString());

                    DataStoreInfo si;
                    StoreInfo preExisting = getCatalog().getStoreByName(workspace, generalParams.name,
                            StoreInfo.class);
                    boolean storeNew = false;
                    if (preExisting != null) {
                        if (!(preExisting instanceof DataStoreInfo)) {
                            error(new ParamResourceModel("storeExistsNotVector", this, generalParams.name));
                            return;
                        }
                        si = (DataStoreInfo) preExisting;
                        if (!si.getType().equals(storeType)
                                || !si.getConnectionParameters().equals(params)) {
                            error(new ParamResourceModel("storeExistsNotSame", this, generalParams.name));
                            return;
                        }
                        // make sure it's enabled, we just verified the directory exists
                        si.setEnabled(true);
                    } else {
                        storeNew = true;
                        CatalogBuilder builder = new CatalogBuilder(getCatalog());
                        builder.setWorkspace(workspace);
                        si = builder.buildDataStore(generalParams.name);
                        si.setDescription(generalParams.description);
                        si.getConnectionParameters().putAll(params);
                        si.setEnabled(true);
                        si.setType(storeType);

                        getCatalog().add(si);
                    }

                    // redirect to the layer chooser
                    PageParameters pp = new PageParameters();
                    pp.put("store", si.getName());
                    pp.put("workspace", workspace.getName());
                    pp.put("storeNew", storeNew);
                    pp.put("workspaceNew", false);
                    setResponsePage(VectorLayerChooserPage.class, pp);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while setting up mass import", e);
                }

            }
        };
    }

    /**
     * Tries to figure out a free project name from a prefix (was meant to work for a random
     * directory name but could not hook it up properly to both dialog navigation and manual
     * directory field filling so I just gave up).
     * 
     * @param prefix
     * @return
     */
    String getProjectNameFromPrefix(String prefix) {
        String name = new File(prefix).getName();
        if (name.length() > 6) {
            name = name.substring(0, 6);
        }

        Catalog catalog = getCatalog();
        String candidate = name;
        for (int i = 1; i < 100; i++) {
            if (catalog.getWorkspaceByName(candidate) == null)
                return candidate;

            if (catalog.getDataStoreByName(candidate, candidate) == null)
                return candidate;

            // build a 6 chars version with a number at the end
            if (i < 10) {
                if (prefix.length() == 6)
                    candidate = prefix.substring(0, 5) + i;
                else
                    candidate = prefix + i;
            } else {
                if (prefix.length() > 4)
                    candidate = prefix.substring(0, 4) + i;
                else
                    candidate = prefix + i;
            }
        }

        return null;
    }

    static class DirectoryValidator extends AbstractValidator {

        @Override
        protected void onValidate(IValidatable validatable) {
            String directory = (String) validatable.getValue();

            DataStore store = null;
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            try {
                // check the store can be built (we need to provide the namespace as well
                params.put(ShapefileDataStoreFactory.URLP.key, new File(directory).toURI().toURL());
                params.put(ShapefileDataStoreFactory.NAMESPACEP.key, new URI(
                        "http://www.geoserver.org"));
                store = DataStoreFinder.getDataStore(params);
                if (store == null) {
                    error(validatable, "ImportPage.invalidPath");
                } else if (store.getTypeNames().length == 0) {
                    error(validatable, "ImportPage.noData");
                }
            } catch (Exception e) {
                error(validatable, "ImportPage.noData");
            }
        }
    }

    class ProjectValidator extends AbstractValidator {

        @Override
        protected void onValidate(IValidatable validatable) {
            String project = (String) validatable.getValue();

            // new workspace? if so, good
            WorkspaceInfo ws = getCatalog().getWorkspaceByName(project);
            if (ws == null)
                return;

            // new store too?
            StoreInfo store = getCatalog().getStoreByName(ws, project, StoreInfo.class);
            if (store != null)
                error(validatable, "ImportPage.duplicateStore", Collections.singletonMap("project",
                        project));
        }

    }

}
