/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.data.importer.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StoreChoiceRenderer;
import org.geoserver.web.data.store.StoreModel;
import org.geoserver.web.data.store.StoresModel;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspaceDetachableModel;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.util.logging.Logging;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.Importer;

/**
 * First page of the import wizard.
 * 
 * @author Andrea Aime - OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class ImportDataPage extends GeoServerSecuredPage {

    static Logger LOGGER = Logging.getLogger(ImportDataPage.class);

    ListView<Source> sourceList;
    WebMarkupContainer sourcePanel;
    
    WorkspaceDetachableModel workspace;
    DropDownChoice workspaceChoice;
    
    StoreModel store;
    DropDownChoice storeChoice;
    
    String storeName;
    TextField storeNameTextField;

    ImportContextTable importTable;

    GeoServerDialog dialog;
    
    public ImportDataPage(PageParameters params) {
        Form form = new Form("form");
        add(form);
        
        sourceList = new ListView<Source>("sources", Arrays.asList(Source.values())) {
            @Override
            protected void populateItem(final ListItem<Source> item) {
                final Source source = (Source) item.getModelObject();
                AjaxLink link = new AjaxLink("link") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        updateSourcePanel(source);
                        updateModalLinks(this, target);
                        target.addComponent(sourcePanel);
                    }
                };
                link.setOutputMarkupId(true);
                
                link.add(new Label("name", source.getName(ImportDataPage.this)));
                if(item == sourceList.get(0)) {
                    link.add(new AttributeAppender("class", true, new Model("selected"), " "));
                }
                item.add(link);

                item.add(new Label("description", source .getDescription(ImportDataPage.this)));

                Image icon = new Image("icon", source.getIcon());
                icon.add(
                    new AttributeModifier("alt", true, source.getDescription(ImportDataPage.this)));
                item.add(icon);

                if (!source.isAvailable()) {
                    item.setEnabled(false);
                    item.add(new SimpleAttributeModifier("title", "Data source not available. Please " +
                        "install required plug-in and drivers."));
                }
            }
            
        };
        form.add(sourceList);
        
        sourcePanel = new WebMarkupContainer("panel");
        sourcePanel.setOutputMarkupId(true);
        form.add(sourcePanel);
        
        Catalog catalog = GeoServerApplication.get().getCatalog();
        
        // workspace chooser
        workspace = new WorkspaceDetachableModel(catalog.getDefaultWorkspace());
        workspaceChoice = new DropDownChoice("workspace", workspace, new WorkspacesModel(), 
            new WorkspaceChoiceRenderer());
        workspaceChoice.setOutputMarkupId(true);
        workspaceChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateDefaultStore(target);
            }
        });
        form.add(workspaceChoice);
        
        //store chooser
        store = new StoreModel(catalog.getDefaultDataStore((WorkspaceInfo) workspace.getObject()));
        storeChoice = new DropDownChoice("store", store, new EnabledStoresModel(workspace),
            new StoreChoiceRenderer()) {
            protected String getNullValidKey() {
                return ImportDataPage.class.getSimpleName() + "." + super.getNullValidKey();
            };
        };
        storeChoice.setOutputMarkupId(true);

        storeChoice.setNullValid(true);
        form.add(storeChoice);
        
        // new workspace
        form.add(new AjaxLink("newWorkspace") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.setTitle(new ParamResourceModel("newWorkspace", ImportDataPage.this));
                dialog.setInitialWidth(400);
                dialog.setInitialHeight(150);
                dialog.setMinimalHeight(150);
                
                dialog.showOkCancel(target, new DialogDelegate() {
                    String wsName;

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        try {
                            Catalog catalog = GeoServerApplication.get().getCatalog();
                            
                            NewWorkspacePanel panel = (NewWorkspacePanel) contents;
                            wsName = panel.workspace;
                            
                            WorkspaceInfo ws = catalog.getFactory().createWorkspace();
                            ws.setName(wsName);
                            
                            NamespaceInfo ns = catalog.getFactory().createNamespace();
                            ns.setPrefix(wsName);
                            ns.setURI("http://opengeo.org/#" + URLEncoder.encode(wsName, "ASCII"));
                            
                            catalog.add( ws );
                            catalog.add( ns );

                            return true;
                        } catch(Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                    
                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        Catalog catalog = GeoServerApplication.get().getCatalog();
                        workspace = new WorkspaceDetachableModel(catalog.getWorkspaceByName(wsName));
                        workspaceChoice.setModel(workspace);
                        target.addComponent(workspaceChoice);
                        target.addComponent(storeChoice);
                    }
                    
                    @Override
                    protected Component getContents(String id) {
                        return new NewWorkspacePanel(id);
                    }
                });
                
            }
        });

        form.add(new AjaxSubmitLink("next", form) {

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedbackPanel);
            }
            
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                
                //first update the button to indicate we are working
                add(new AttributeAppender("class", true, new Model("button-working icon"), " "));
                setEnabled(false);
                get(0).setDefaultModelObject("Working");

                target.addComponent(this);

                //start a timer to actually do the work, which will allow the link to update 
                // while the context is created
                final AjaxSubmitLink self = this;
                this.add(new AbstractAjaxTimerBehavior(Duration.milliseconds(100)) {
                    @Override
                    protected void onTimer(AjaxRequestTarget target) {
                        ImportSourcePanel panel = (ImportSourcePanel) sourcePanel.get("content");
                        ImportData source;
                        try {
                            source = panel.createImportSource();
                        } catch (IOException e) {
                            throw new WicketRuntimeException(e);
                        }
                        WorkspaceInfo targetWorkspace = (WorkspaceInfo) 
                            (workspace.getObject() != null ? workspace.getObject() : null);
                        StoreInfo targetStore = (StoreInfo) (store.getObject() != null ? store
                                .getObject() : null);

                        Importer importer = ImporterWebUtils.importer();
                        try {
                            ImportContext imp = importer.createContext(source, targetWorkspace,
                                    targetStore);

                            //check the import for actual things to do
                            boolean proceed = !imp.getTasks().isEmpty();
                            if (proceed) {
                                //check that all the tasks are non-empty
                                proceed = false;
                                for (ImportTask t : imp.getTasks()) {
                                    if (!t.getItems().isEmpty()) {
                                        proceed = true;
                                        break;
                                    }
                                }
                            }

                            if (proceed) {
                                imp.setArchive(false);
                                importer.changed(imp);
    
                                PageParameters pp = new PageParameters();
                                pp.put("id", imp.getId());
    
                                setResponsePage(ImportPage.class, pp);
                            }
                            else {
                                info("No data to import was found");
                                target.addComponent(feedbackPanel);

                                importer.delete(imp);

                                resetNextButton(self, target);
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
                            error(e);

                            target.addComponent(feedbackPanel);

                            //update the button back to original state
                            resetNextButton(self, target);
                        }
                        finally {
                            stop();
                        }
                    }
                });
            }
        }.add(new Label("message", new Model("Next"))));

        importTable = new ImportContextTable("imports", new ImportContextProvider() {
            @Override
            protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<ImportContext>> getProperties() {
                return Arrays.asList(ID, CREATED, STATE);
            }
        });
        importTable.setOutputMarkupId(true);
        importTable.setFilterable(false);
        importTable.setSortable(false);
        form.add(importTable);

        form.add(new AjaxLink("removeAll") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                Importer importer = ImporterWebUtils.importer();
                importer.getStore().removeAll();
                target.addComponent(importTable);
            }
        }.setVisible(ImporterWebUtils.isDevMode()));
        
        add(dialog = new GeoServerDialog("dialog"));
        
        updateSourcePanel(Source.SPATIAL_FILES);
        updateDefaultStore(null);
    }

    void updateDefaultStore(AjaxRequestTarget target) {
        WorkspaceInfo ws = (WorkspaceInfo) workspace.getObject();
        if (workspace != null) {
            store.setObject(GeoServerApplication.get().getCatalog().getDefaultDataStore(ws));
        }

        if (target != null) {
            target.addComponent(storeChoice);
        }
    }

    void updateSourcePanel(Source source) {
        Panel old = (Panel) sourcePanel.get(0);
        if (old != null) {
            sourcePanel.remove(old);
        }

        Panel p = source.createPanel("content");
        sourcePanel.add(p);
    }
    
    void updateModalLinks(AjaxLink selected, AjaxRequestTarget target) {
        int n = sourceList.getModelObject().size();
        for (int i = 0; i < n; i++) {
            AjaxLink link = (AjaxLink) ((ListItem)sourceList.get(i)).get("link");
            if (link == selected) {
                link.add(new AttributeAppender("class", new Model("selected"), " "));
            }
            else {
                link.add(new SimpleAttributeModifier("class", ""));
            }
            target.addComponent(link);
        }
    }

    void resetNextButton(AjaxSubmitLink next, AjaxRequestTarget target) {
        next.add(new AttributeModifier("class", true, new Model("")));
        next.setEnabled(true);
        next.get(0).setDefaultModelObject("Next");
        target.addComponent(next);
    }

    /**
     * A type data source.
     */
    enum Source {
        SPATIAL_FILES(DataIcon.FOLDER) {
            @Override
            ImportSourcePanel createPanel(String panelId) {
                return new SpatialFilePanel(panelId);
            }
        },
        POSTGIS(DataIcon.POSTGIS) {
            @Override
            ImportSourcePanel createPanel(String panelId) {
                return new PostGISPanel(panelId);
            }  
        }, 
        ORACLE(DataIcon.DATABASE) { 
            @Override
            ImportSourcePanel createPanel(String panelId) {
                return new OraclePanel(panelId);
            }

            @Override
            boolean isAvailable() {
                return isDataStoreFactoryAvaiable("org.geotools.data.oracle.OracleNGDataStoreFactory");
            }
        }, 
        SQLSERVER(DataIcon.DATABASE) {
            @Override
            ImportSourcePanel createPanel(String panelId) {
                return new SQLServerPanel(panelId);
            }

            @Override
            boolean isAvailable() {
                return isDataStoreFactoryAvaiable("org.geotools.data.sqlserver.SQLServerDataStoreFactory");
            }
        };
        
//        directory(new ResourceReference(GeoServerApplication.class, "img/icons/silk/folder.png"),
//                DirectoryPage.class, "org.geotools.data.shapefile.ShapefileDataStoreFactory"), // 
//        postgis(new ResourceReference(GeoServerApplication.class,
//                "img/icons/geosilk/database_vector.png"), PostGISPage.class,
//                "org.geotools.data.postgis.PostgisNGDataStoreFactory"), //
//        oracle(new ResourceReference(GeoServerApplication.class,
//                "img/icons/geosilk/database_vector.png"), OraclePage.class,
//                "org.geotools.data.oracle.OracleNGDataStoreFactory"), //
//        sqlserver(new ResourceReference(GeoServerApplication.class,
//                "img/icons/geosilk/database_vector.png"), SQLServerPage.class,
//                "org.geotools.data.sqlserver.SQLServerDataStoreFactory"), //
//        arcsde(new ResourceReference(GeoServerApplication.class,
//                "img/icons/geosilk/database_vector.png"), ArcSDEPage.class,
//                "org.geotools.arcsde.ArcSDEDataStoreFactory");

        DataIcon icon;

        Source(DataIcon icon) {
            this.icon = icon;
        }

        IModel getName(Component component) {
            return new ParamResourceModel(this.name().toLowerCase() + "_name", component);
        }

        IModel getDescription(Component component) {
            return new ParamResourceModel(this.name().toLowerCase() + "_description", component);
        }

        ResourceReference getIcon() {
            return icon.getIcon();
        }

        boolean isAvailable() {
            return true;
        }

        boolean isDataStoreFactoryAvaiable(String className) {
            Class<DataStoreFactorySpi> clazz = null;
            try {
                clazz = (Class<DataStoreFactorySpi>) Class.forName(className);
            }
            catch(Exception e) {
                if(LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "DataStore class not available: " + className, e);
                }
            }
            if (clazz == null) {
                return false;
            }

            DataStoreFactorySpi factory = null;
            try {
                factory = clazz.newInstance();
            }
            catch(Exception e) {
                if(LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Error creating DataStore factory: " + className, e);
                }
            }

            if (factory == null) {
                return false;
            }

            return factory.isAvailable();
        }

        abstract ImportSourcePanel createPanel(String panelId);
    }
}
