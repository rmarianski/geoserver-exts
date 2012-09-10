package org.opengeo.data.importer.web;

import static org.opengeo.data.importer.web.ImporterWebUtils.importer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.time.Duration;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.Icon;
import org.opengeo.data.importer.BasicImportFilter;
import org.opengeo.data.importer.Database;
import org.opengeo.data.importer.Directory;
import org.opengeo.data.importer.FileData;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportData;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.ImportTask.State;
import org.opengeo.data.importer.RasterFormat;
import org.opengeo.data.importer.VectorFormat;
import org.opengeo.data.importer.job.Task;

public class ImportPage extends GeoServerSecuredPage {

    GeoServerDialog dialog;

    public ImportPage(PageParameters pp) {
        this(new ImportContextModel(pp.getAsLong("id")));
    }

    public ImportPage(ImportContext imp) {
        this(new ImportContextModel(imp));
    }

    public ImportPage(IModel<ImportContext> model) {
        initComponents(model);
    }

    void initComponents(final IModel<ImportContext> model) {
        add(new Label("id", new PropertyModel(model, "id")));

        ImportContextProvider provider = new ImportContextProvider() {
            @Override
            protected List<Property<ImportContext>> getProperties() {
                return Arrays.asList(STATE, CREATED, UPDATED);
            }
            @Override
            protected List<ImportContext> getItems() {
                return Collections.singletonList(model.getObject());
            }
        };

        add(new AjaxLink("raw") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                dialog.setInitialHeight(500);
                dialog.setInitialWidth(700);
                dialog.showOkCancel(target, new DialogDelegate() {
                    @Override
                    protected Component getContents(String id) {
                        XStreamPersister xp = importer().createXStreamPersister();
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        try {
                            xp.save(model.getObject(), bout);
                        } catch (IOException e) {
                            bout = new ByteArrayOutputStream();
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                            e.printStackTrace(new PrintWriter(bout));
                        }

                        return new TextAreaPanel(id, new Model(new String(bout.toByteArray())));
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target,  Component contents) {
                        return true;
                    }
                });
            }
        }.setVisible(ImporterWebUtils.isDevMode()));

        final ImportContextTable headerTable = new ImportContextTable("header", provider);
        headerTable.setOutputMarkupId(true);
        headerTable.setFilterable(false);
        headerTable.setPageable(false);
        add(headerTable);

        final ImportContext imp = model.getObject();
        ListView<ImportTask> tasksView = new ListView<ImportTask>("tasks", 
            new FilteredImportTasksModel(new ImportTasksModel(imp), false)) {
        //ListView<ImportTask> tasksView = new ListView<ImportTask>("tasks", new ImportTasksDetachableModel(imp)) {
            @Override
            protected void populateItem(final ListItem<ImportTask> item) {
                IModel<ImportTask> model = item.getModel();

                ImportTask task = item.getModelObject();
                ImportData data = task.getData();

                item.add(new Icon("icon",new DataIconModel(data)));
                item.add(new Label("title", new DataTitleModel(task))
                    .add(new AttributeModifier("title", new DataTitleModel(task, false))));

                StoreInfo store = task.getStore();
                if (store != null) {
                    String targetModelKey = null;
                    switch(task.getState()) {
                    case COMPLETE:
                        targetModelKey = "importCompleted";
                        break;
                    default:
                        targetModelKey = store.getId()==null?"importNewStore":"importExistingStore"; 
                    }

                    item.add(new Label("target",
                        new StringResourceModel(targetModelKey, ImportPage.this, null)));
                    item.add(new Link("targetLink") {
                        @Override
                        public void onClick() {
                            GeoServerBasePage page = null;
                            StoreInfo store = item.getModelObject().getStore();
                            
                            if (store instanceof DataStoreInfo) {
                                page = new DataStoreEditPage((DataStoreInfo) store);
                            }
                            else if (store instanceof CoverageStoreInfo) {
                                page = new CoverageStoreEditPage((CoverageStoreInfo)store);
                            }
                            if (page != null) {
                                page.setReturnPage(ImportPage.this);
                                setResponsePage(page);
                            }
                        }
                    }.add(new Label("store", new PropertyModel(store, "name"))));
                }
                else {
                    //dummy data
                    item.add(new Label("target", "").setVisible(false));
                    item.add(new Link("targetLink") {
                        @Override
                        public void onClick() {
                        }
                    }.add(new Label("name", "")).setVisible(false));
                }

                //ImportItemProvider provider = new ImportItemProvider(item.getModelObject());
                GeoServerDataProvider<ImportItem> provider = new GeoServerDataProvider<ImportItem>() {

                    @Override
                    protected List<Property<ImportItem>> getProperties() {
                        return new ImportItemProvider((IModel)null).getProperties();
                    }

                    @Override
                    protected List<ImportItem> getItems() {
                        return item.getModelObject().getItems();
                    }
                    
                };

                boolean selectable = task.getState() != ImportTask.State.COMPLETE;
                final ImportItemTable itemTable = new ImportItemTable("items", provider, selectable) {
                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        updateImportLink((AjaxLink) item.get("import"), this, target);
                    }
                }.setFeedbackPanel(feedbackPanel);
                item.add(itemTable);
                
                itemTable.setOutputMarkupId(true);
                itemTable.setFilterable(false);
                itemTable.setSortable(false);

                final AjaxLink<Long> importLink = new AjaxLink<Long>("import", new Model<Long>()) {
                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        ImporterWebUtils.disableLink(tag);
                    }
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ImportTask task = item.getModelObject();

                        BasicImportFilter filter = new BasicImportFilter();
                        filter.add(task, itemTable.getSelection());

                        final Long jobid = 
                            importer().runAsync(task.getContext(), filter);
                        setDefaultModelObject(jobid);

                        final AjaxLink self = this;

                        //create a timer to update the table and reload the page when necessary
                        itemTable.add(new AbstractAjaxTimerBehavior(Duration.milliseconds(500)) {
                            @Override
                            protected void onTimer(AjaxRequestTarget target) {
                                Task<ImportContext> job = importer().getTask(jobid); 
                                if (job == null || job.isDone()) {
                                    //remove the timer
                                    stop();
                                    
                                    self.setEnabled(true);
                                    target.addComponent(self);

                                    ImportTask task = item.getModelObject();
                                    if (task.getState() == State.COMPLETE) {
                                        //enable cancel, which will not be "done"
                                        setLinkEnabled(cancelLink(self), true, target);
                                    }
                                    else {
                                        //disable cancel, import is not longer running, but also 
                                        // not complete
                                        setLinkEnabled(cancelLink(self), false, target);
                                    }
                                }

                                //update the table
                                target.addComponent(itemTable);
                                target.addComponent(headerTable);
                            }
                        });
                        target.addComponent(itemTable);

                        //disable import button
                        setLinkEnabled(this, false, target);
                        //enable cancel button
                        setLinkEnabled(cancelLink(this), true, target);
                    }
                };
                importLink.setOutputMarkupId(true);
                importLink.setEnabled(doSelectReady(task, itemTable, null));
                item.add(importLink);

                final AjaxLink cancelLink = new AjaxLink("cancel") {
                    @Override
                    protected void disableLink(ComponentTag tag) {
                        super.disableLink(tag);
                        ImporterWebUtils.disableLink(tag);
                    }
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ImportTask t = item.getModelObject();
                        if (t.getState() == State.COMPLETE) {
                            setResponsePage(ImportDataPage.class);
                            return;
                        }

                        Long jobid = importLink.getModelObject();
                        if (jobid == null) {
                            return;
                        }

                        Task<ImportContext> task = importer().getTask(jobid);
                        if (task == null || task.isDone()) {
                            return;
                        }

                        task.getMonitor().setCanceled(true);
                        task.cancel(false);
                        try {
                            task.get();
                        }
                        catch(Exception e) {
                        }

                        //enable import button
                        setLinkEnabled(importLink, true, target);
                        //disable cancel button
                        setLinkEnabled(cancelLink(importLink), false, target);
                    }
                    
                };
                cancelLink.setEnabled(task.getState() == State.COMPLETE);
                cancelLink.add(new Label("text", new CancelTitleModel(model)));
                item.add(cancelLink);
                item.add(new AjaxLink<ImportTask>("select-all", model) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        itemTable.selectAll();
                        target.addComponent(itemTable);
                        updateImportLink(importLink, itemTable, target);
                    }
                });
                item.add(new AjaxLink<ImportTask>("select-none", model) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        itemTable.clearSelection();
                        target.addComponent(itemTable);
                        updateImportLink(importLink, itemTable, target);
                    }
                });
                item.add(new AjaxLink<ImportTask>("select-ready", model) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doSelectReady(getModelObject(), itemTable, target);
                        updateImportLink(importLink, itemTable, target);
                    }
                });
            }
        };
        add(tasksView);

        ListView<ImportTask> emptyTasksView = new ListView<ImportTask>("emptyTasks", 
                new FilteredImportTasksModel(new ImportTasksModel(imp), true)) {
            @Override
            protected void populateItem(ListItem<ImportTask> item) {
                item.add(new Label("title", new DataTitleModel(item.getModelObject())));
            }
        };
        add(emptyTasksView);

        add(dialog = new GeoServerDialog("dialog"));

    }

    void updateImportLink(AjaxLink link, ImportItemTable table, AjaxRequestTarget target) {
        boolean enable = !table.getSelection().isEmpty();
        if (enable) {
            boolean allComplete = true;
            for (ImportItem item : table.getSelection()) {
                allComplete = item.getState() == ImportItem.State.COMPLETE;
            }
            enable = !allComplete;
        }

        setLinkEnabled(link, enable, target);
    }

    void setLinkEnabled(AjaxLink link, boolean enabled, AjaxRequestTarget target) {
        link.setEnabled(enabled);
        target.addComponent(link);
    }

    AjaxLink cancelLink(AjaxLink importLink) {
        return (AjaxLink) importLink.getParent().get("cancel");
    }

    boolean doSelectReady(ImportTask task, ImportItemTable table, AjaxRequestTarget target) {
        boolean empty = true;
        List<ImportItem> items = task.getItems();
        
        for (int i = 0; i < items.size() && i < 25; i++) {
            ImportItem item = items.get(i);
            if (item.getState() == ImportItem.State.READY) {
                //table.selectObject(item);
                table.selectIndex(i);
                empty = false;
            }
        }
        if (target != null) {
            target.addComponent(table);
        }
        return !empty;
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return null;
    }

    static class DataIconModel extends LoadableDetachableModel<ResourceReference> {

        ImportData data;

        public DataIconModel(ImportData data) {
            this.data = data;
        }

        @Override
        protected ResourceReference load() {
            DataIcon icon = null;
            if (data instanceof FileData) {
                FileData df = (FileData) data;
                if (data instanceof Directory) {
                    icon = DataIcon.FOLDER;
                }
                else {
                    icon = df.getFormat() instanceof VectorFormat ? DataIcon.FILE_VECTOR : 
                           df.getFormat() instanceof RasterFormat ? DataIcon.FILE_RASTER : DataIcon.FILE;
                }
            }
            else if (data instanceof Database) {
                icon = DataIcon.DATABASE;
            }
            else {
                icon = DataIcon.VECTOR; //TODO: better default
            }
            return icon.getIcon();
        }
    
    }

    static class DataTitleModel extends LoadableDetachableModel<String> {

        ImportTask task;
        boolean abbrev;
        
        DataTitleModel(ImportTask task) {
            this(task, true);
        }

        DataTitleModel(ImportTask task, boolean abbrev) {
            this.task = task;
            this.abbrev = abbrev;
        }

        @Override
        protected String load() {
            ImportData data = task.getData();

            String title = data.toString();
            if (data instanceof FileData) {
                FileData df = (FileData) data;
                ImportData parentData = task.getContext().getData();
                if (parentData instanceof Directory) {
                    try {
                        title = df.relativePath((Directory) parentData);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }
                }
            }
            if (abbrev && title.length() > 70) {
                //shorten it
                title = title.substring(0,20) + "[...]" + title.substring(title.length()-50);
            }
            return title;
        }
    
    }

    static class CancelTitleModel extends LoadableDetachableModel {

        IModel<ImportTask> taskModel;

        CancelTitleModel(IModel<ImportTask> taskModel) {
            this.taskModel = taskModel;
        }

        @Override
        protected Object load() {
            switch(taskModel.getObject().getState()) {
            case COMPLETE:
                return new StringResourceModel("done", new Model("Done")).getString();
            default:
                return new StringResourceModel("cancel", new Model("Cancel")).getString();
            }
        }

        @Override
        protected void onDetach() {
            super.onDetach();
            taskModel.detach();
        }
    }

    static class TextAreaPanel extends Panel {

        public TextAreaPanel(String id, IModel textAreaModel) {
            super(id);
            
            add(new TextArea("textArea", textAreaModel));
        }
    
    }

    static class FilteredImportTasksModel extends ListModel<ImportTask> {

        IModel<List<ImportTask>> taskModel;
        boolean empty;

        FilteredImportTasksModel(IModel<List<ImportTask>> taskModel, boolean empty) {
            this.taskModel = taskModel;
            this.empty = empty;
        }

        @Override
        public List<ImportTask> getObject() {
            List<ImportTask> tasks = new ArrayList();
            for (ImportTask task : taskModel.getObject()) {
                if (empty != task.getItems().isEmpty()) {
                    continue;
                }
                tasks.add(task);
            }
            return tasks;
        }
    
        @Override
        public void detach() {
            super.detach();
            taskModel.detach();
        }
    }
}
