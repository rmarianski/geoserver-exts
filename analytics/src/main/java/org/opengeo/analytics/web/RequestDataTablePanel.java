package org.opengeo.analytics.web;

import static org.opengeo.analytics.web.RequestDataProvider.*;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestData.Status;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

public class RequestDataTablePanel extends GeoServerTablePanel<RequestData> {

    ModalWindow popupWindow;
    
    public RequestDataTablePanel(String id, GeoServerDataProvider<RequestData> dataProvider) {
        super(id, dataProvider);
        setFilterable(false);
        setSortable(false);
        
        add(popupWindow = new ModalWindow("popup"));
        popupWindow.setInitialHeight(400);
        popupWindow.setInitialWidth(400);
    }

    @Override
    protected Component getComponentForProperty(String id, IModel itemModel, Property property) {
        Object obj = property.getModel(itemModel).getObject();
        if (obj == null) {
            return new Label(id, "");
        }
        
        if (property == PATH || property == SERVICE || property == SERVICE_EXT || 
            property == OPERATION || property == OWS_VERSION || property == START_TIME || 
            property == END_TIME) {
            return new Label(id, property.getModel(itemModel));
        }
        
        if (property == ID) {
            IModel model = property.getModel(itemModel);
            return new SimpleBookmarkableLink(id, RequestPage.class, model,
               "id", model.getObject().toString());
        }
        
        if (property == STATUS) {
            Status status = (Status) obj;
            Label label = new Label(id, status.toString());
            
            if (status == Status.FAILED) {
                label.add(new AttributeAppender("style", new Model("color:red;"), ""));
            }
            
            return label; 
        }
        
        if (property == QUERY_STRING) {
            String queryString = (String) obj;
            return createPreviewLink(id, queryString);
        }
        
        if (property == BODY) {
            byte[] body = (byte[]) obj;
            try {
                return createPreviewLink(id, new String(body, "UTF-8"));
            } 
            catch (UnsupportedEncodingException e) {}
        }
        
        if (property == RESOURCES) {
            List<String> layers = (List<String>) obj;
            if (layers.isEmpty()) {
                return new Label(id, "");
                
            }
            StringBuffer sb = new StringBuffer();
            for (String layer : layers) {
                sb.append(layer).append(",");
            }
            sb.setLength(sb.length()-1);
            
            return createPreviewLink(id, sb.toString());
        }
        if (property == ERROR) {
            String error = (String) obj;
            return createPreviewLink(id, error);
        }
        
        if (property == TOTAL_TIME) {
            Object time = property.getModel(itemModel).getObject();
            if (time != null) { // could still be running
                return new Label(id, time.toString() + " ms");
            }
            return new Label(id);
        }
        return null;
    }

    Component createPreviewLink(String id, final String queryString) {
        if (queryString.length() < 15) {
            return new Label(id, queryString);
        }
        else {
            final String preview = queryString.substring(0, 15) + "...";
            return new SimpleAjaxLink(id, new Model(preview)) {
                @Override
                protected void onClick(AjaxRequestTarget target) {
                    popupWindow.setContent(new PreviewPanel(popupWindow.getContentId(), queryString));
                    popupWindow.show(target);
                }
            };
        }
    }
    
    class PreviewPanel extends Panel {

        public PreviewPanel(String id, String content) {
            super(id);
            
            add(new Label("content", content));
        }
    }

}
