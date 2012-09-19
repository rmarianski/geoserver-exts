package org.opengeo.data.importer.web;

import static org.geoserver.ows.util.ResponseUtils.urlEncode;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.demo.PreviewLayer;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SRSToCRSModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.Importer;
import org.opengeo.data.importer.ImportItem.State;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.FactoryException;

public class ImportItemTable extends GeoServerTablePanel<ImportItem> {

    static Logger LOGGER = Logging.getLogger(Importer.class);
    static CoordinateReferenceSystem EPSG_3857() throws FactoryException {
        return CRS.decode("EPSG:3857");
    }

    ModalWindow popupWindow;
    GeoServerDialog dialog;
    FeedbackPanel feedbackPanel;

    public ImportItemTable(String id, GeoServerDataProvider<ImportItem> dataProvider, boolean selectable) {
        super(id, dataProvider, selectable);
        add(dialog = new GeoServerDialog("dialog"));
        add(popupWindow = new ModalWindow("popup"));
        ((DataView)get("listContainer:items")).setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());
    }

    public ImportItemTable setFeedbackPanel(FeedbackPanel feedbackPanel) {
        this.feedbackPanel = feedbackPanel;
        return this;
    }
    @Override
    protected Component getComponentForProperty(String id, final IModel itemModel, Property property) {
        if (property == ImportItemProvider.NAME) {
              return new LayerLinkPanel(id, itemModel);
        }

        if (property == ImportItemProvider.STATUS) {
            ImportItem.State state = (State) property.getModel(itemModel).getObject();
            Component c = null;
            if (state == ImportItem.State.ERROR) {
                c = new SimpleAjaxLink<ImportItem>(id, itemModel, new StatusDescriptionModel(property.getModel(itemModel))) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        popupWindow.setContent(
                                new ExceptionPanel(popupWindow.getContentId(), getModelObject().getError()));
                        popupWindow.show(target);
                    }
                };
            }
            else {
                
                c = new Label(id, new StatusDescriptionModel(property.getModel(itemModel)));
            }
            
            String cssClass = new StatusIconModel(property.getModel(itemModel)).getCssClass();
            return c.add(new SimpleAttributeModifier("class", cssClass));
        }
        if (property == ImportItemProvider.ACTION) {
            
            ImportItem.State state = (State) property.getModel(itemModel).getObject();
            switch(state) {
                case COMPLETE:
                    //link to map preview
                    return new LayerPreviewPanel(id, itemModel);
                case NO_CRS:
                    //provide link to choose crs
                    return new NoCRSPanel(id, itemModel);
                    //return createFixCRSLink(id, itemModel);
                case READY:
                    //return advanced option link
                    //for now disable if this is not a vector layer
                    ImportItem item = (ImportItem) itemModel.getObject();
                    if (item.getLayer() != null && item.getLayer().getResource() instanceof FeatureTypeInfo) {
                        return new AdvancedOptionPanel(id, itemModel);    
                    }
                    return new WebMarkupContainer(id);
                default:
                    return new WebMarkupContainer(id);
            }
        }
        return null;
    }

    SimpleAjaxLink createFixCRSLink(String id, final IModel<ImportItem> itemModel) {
        return new SimpleAjaxLink(id, new Model("Fix...")) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                dialog.showOkCancel(target, new DialogDelegate() {

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        ImporterWebUtils.importer().changed(itemModel.getObject());
                        target.addComponent(ImportItemTable.this);
                        return true;
                    }

                    @Override
                    protected Component getContents(String id) {
                        return new NoCRSPanel(id, 
                            new SRSToCRSModel(new PropertyModel(itemModel, "layer.resource.sRS")));
                    }
                });
            }
        };
    }

    protected void onItemFixed(ImportItem item, AjaxRequestTarget target) {
        selectObject(item);
        target.addComponent(this);
        onSelectionUpdate(target);
    }

    static abstract class StatusModel<T> implements IChainingModel<T> {
        
        IModel chained;

        protected StatusModel(IModel model) {
            this.chained = model;
        }
        
        public void setObject(T object) {
        }

        public void detach() {
            chained.detach();
        }

        public void setChainedModel(IModel<?> model) {
            this.chained = model;
        }

        public IModel<?> getChainedModel() {
            return chained;
        }
    }
    static class StatusIconModel extends StatusModel<ResourceReference> {

        StatusIconModel(IModel model) {
            super(model);
        }
        
        public ResourceReference getObject() {
            ImportItem.State state = (ImportItem.State) chained.getObject();
            switch(state) {
            case READY:
                return new ResourceReference(GeoServerApplication.class, "img/icons/silk/bullet_go.png");
            case RUNNING:
                return new ResourceReference(ImportItemTable.class, "indicator.gif");
            case COMPLETE:
                return new ResourceReference(GeoServerApplication.class, "img/icons/silk/accept.png");
            case NO_BOUNDS:
            case NO_CRS:
            //case NO_FORMAT:
                return new ResourceReference(GeoServerApplication.class, "img/icons/silk/error.png");
            case ERROR:
                return new ResourceReference(GeoServerApplication.class, "img/icons/silk/delete.png");
            }
            return null;
        }
        public String getCssClass() {
            ImportItem.State state = (ImportItem.State) chained.getObject();
            switch(state) {
            case READY:
                return "apply-link";
            case RUNNING:
                return "working-link";
            case COMPLETE:
                return "accept-link";
            case NO_BOUNDS:
            case NO_CRS:
            case ERROR:
            //case NO_FORMAT:
                return "warning-link";
            //case ERROR:
            //    return "error-link";
            case CANCELED:
                return "cancel-link";
            }
            return "";
        }
    }
    class StatusDescriptionModel extends StatusModel<String> {

        StatusDescriptionModel(IModel model) {
            super(model);
        }

        public String getObject() {
            ImportItem.State state = (ImportItem.State) chained.getObject();
            return new StringResourceModel(
                state.name().toLowerCase(), ImportItemTable.this, null).getString();
        }
    }

//    class FormatDelegate extends DialogDelegate {
//
//        LayerSummary layer;
//        
//        public FormatDelegate(LayerSummary layer) {
//            this.layer = layer;
//        }
//        
//        @Override
//        protected Component getContents(String id) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
//
//        }
//    }
    
//    static class FormatPanel extends Panel {
//
//        public FormatPanel(String id, IModel model) {
//            super(id);
//            add(new FormatDropDownChoice("format", model).add(
//                new AjaxFormComponentUpdatingBehavior("onchange") {
//                    @Override
//                    protected void onUpdate(AjaxRequestTarget target) {}
//                }));
//            add(new AjaxLink("apply") {
//                @Override
//                public void onClick(AjaxRequestTarget target) {
//                    onApply(target);
//                }
//            });
//        }
//        
//        protected void onApply(AjaxRequestTarget target) {
//        }
//    }
//    
//    static class FormatDropDownChoice extends DropDownChoice<LayerFormat> {
//
//        public FormatDropDownChoice(String id, IModel model) {
//            super(id, model, new FormatsModel());
//            setChoiceRenderer(new IChoiceRenderer<LayerFormat>() {
//                public Object getDisplayValue(LayerFormat object) {
//                    return getIdValue(object, -1);
//                }
//
//                public String getIdValue(LayerFormat object, int index) {
//                    return object.getName();
//                }
//            });
//        }
//    }
//    
//    static class FormatsModel extends LoadableDetachableModel<List<LayerFormat>> {
//
//        @Override
//        protected List<LayerFormat> load() {
//            return LayerFormat.all();
//        }
//        
//    }
//
    class NoCRSPanel extends Panel {

        public NoCRSPanel(String id, final IModel<ImportItem> model) {
            super(id, model);

            Form form = new Form("form");
            add(form);

            form.add(new CRSPanel("crs", 
                new SRSToCRSModel(new PropertyModel(model, "layer.resource.sRS"))) {
                @Override
                protected CoordinateReferenceSystem fromSRS(String srs) {
                    try {
                        return CRS.decode(srs);
                    }
                    catch(Exception e) {
                        error(e);
                        return null;
                    }
                }
            });

            form.add(new AjaxSubmitLink("apply") {
                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    target.addComponent(feedbackPanel);
                }
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    target.addComponent(feedbackPanel);
                    ImportItem item = model.getObject();
                    ImporterWebUtils.importer().changed(item);

                    //ImportItemTable.this.modelChanged();
                    target.addComponent(ImportItemTable.this);
                    onItemFixed(item, target);
                }
            });
        }
    }

    static class LayerLinkPanel extends Panel {
        public LayerLinkPanel(String id, final IModel<ImportItem> model) {
            super(id);
            
            add(new Link<ImportItem>("link", model) {
                @Override
                public void onClick() {
                    ImportItem item = getModelObject();

                    PageParameters pp = new PageParameters();
                    pp.put("id", item.getTask().getContext().getId());

                    setResponsePage(new LayerPage(item.getLayer(), pp) {
                        protected void onSuccessfulSave() {
                            super.onSuccessfulSave();

                            //update the item
                            ImporterWebUtils.importer().changed(model.getObject());
                        };
                    });
                }
            }.add(new Label("name", new PropertyModel(model, "layer.name"))));
        }
    }

    class LayerPreviewPanel extends Panel {
        public LayerPreviewPanel(String id, IModel<ImportItem> model) {
            super(id);
            
            LayerInfo layer = model.getObject().getLayer();
            PreviewLayer preview = new PreviewLayer(layer);

            List<PreviewLink> links = new ArrayList<PreviewLink>();
            links.add(new PreviewLink("layerPreview", preview.getWmsLink()+ "&format=application/openlayers"));

            String gxpLink = 
                    System.getProperty("opengeo.geoexplorer.url", "/geoexplorer");
            gxpLink = gxpLink.endsWith("/") ? gxpLink.substring(0,gxpLink.length()-1) : gxpLink;

            gxpLink += "/composer/?layers=" +
                urlEncode(layer.getResource().getStore().getWorkspace().getName() + ":" +  layer.getName());

            //geoexplorer needs bbox in spherical mercator
            try {
                ReferencedEnvelope e = layer.getResource().getLatLonBoundingBox().transform(EPSG_3857(), true);
                if (e != null) {
                    gxpLink += "&bbox=" + 
                            String.format("%f,%f,%f,%f", e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY()); 
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to reproject "+ layer.getName() + " to spherical mercator", e);
            }
            links.add(new PreviewLink("geoexplorer", gxpLink)); 
            links.add(new PreviewLink("googleearth", "../wms/kml?layers=" + layer.getName()));

            add(new DropDownChoice<PreviewLink>("links", new Model(links.get(0)), links, 
                new ChoiceRenderer<PreviewLink>() {
                @Override
                public Object getDisplayValue(PreviewLink object) {
                    return new ParamResourceModel(object.id, ImportItemTable.this, object.id).getString();
                }
                @Override
                public String getIdValue(PreviewLink object, int index) {
                    return object.href;
                }
            }).setNullValid(false).setOutputMarkupId(true));

            add(new ExternalLink("go","#").add(new AttributeModifier("onclick", 
                new Model("go(document.getElementById('" + get("links").getMarkupId() + "'));"))));
       }

        class PreviewLink implements Serializable {
           String id;
           String href;
           PreviewLink(String id, String href) {
               this.id = id;
               this.href = href;
           }
        }
    }

    static class AdvancedOptionPanel extends Panel {
        public AdvancedOptionPanel(String id, IModel<ImportItem> model) {
            super(id);
            
            add(new Link<ImportItem>("link", model) {
                @Override
                public void onClick() {
                    setResponsePage(new ImportItemAdvancedPage(getModel()));
                }
            });
        }
    }

    static class ErrorPanel extends Panel {
        ModalWindow popupWindow;

        public ErrorPanel(String id, IModel<ImportItem> model) {
            super(id);
    
            add(popupWindow = new ModalWindow("popup"));
            add(new AjaxLink<ImportItem>("link", model) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    popupWindow.setContent(
                        new ExceptionPanel(popupWindow.getContentId(), getModelObject().getError()));
                    popupWindow.show(target);
                }
            });
        }
    }

    static class ExceptionPanel extends Panel {

        public ExceptionPanel(String id, final Exception ex) {
            super(id);
            add(new Label("message", ex.getLocalizedMessage()));
            add(new TextArea("stackTrace", new Model(handleStackTrace(ex))));
            add(new AjaxLink("copy") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    String text = handleStackTrace(ex);
                    StringSelection selection = new StringSelection(text);
                    Toolkit.getDefaultToolkit()
                        .getSystemClipboard().setContents(selection, selection);
                }
            });
        }

        String handleStackTrace(Exception ex) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(out);
            ex.printStackTrace(writer);
            writer.flush();
            
            return new String(out.toByteArray());
        }
    }
}
