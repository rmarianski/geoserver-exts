package org.opengeo.analytics.web;

import org.apache.wicket.PageMap;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.PageParameters;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.web.MonitorBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Point;

import freemarker.template.SimpleHash;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.PopupSettings;
import static org.opengeo.analytics.web.RequestDataProvider.*;

public class RequestPage extends MonitorBasePage {

    RequestData request;
    
    public RequestPage(PageParameters params) {
        this(params.getAsLong("id", -1l));
    }
    
    public RequestPage(long id) {
        this(Analytics.monitor().getDAO().getRequest(id));
    }
    
    public RequestPage(final RequestData request) {
        this.request = request;
 
        add(new Label("id", new PropertyModel(request, "id")));
        
        GeoServerDataProvider<RequestData> provider = new GeoServerDataProvider<RequestData>() {
            @Override
            protected List<Property<RequestData>> getProperties() {
                return Arrays.asList(PATH, HTTP_METHOD, SERVICE_EXT, OPERATION, 
                    START_TIME, STATUS, TOTAL_TIME);
            }

            @Override
            protected List<RequestData> getItems() {
                return Collections.singletonList(request);
            }
        };
        RequestDataTablePanel headerTable = new RequestDataTablePanel("header", provider);
        headerTable.setPageable(false);
        headerTable.setFilterable(false);
        add(headerTable);
        
        List<Class<? extends RequestPanel>> panels = new ArrayList();
        
        if (request.getQueryString() != null) {
            panels.add(QueryStringPanel.class);
        }
        
        if (request.getBody() != null) {
            panels.add(BodyPanel.class);
        }
        
        if (request.getResponseLength() > 0) {
            panels.add(ResponsePanel.class);
        }
        
        if (request.getError() != null) {
            panels.add(ErrorPanel.class);
        }
        panels.add(OriginPanel.class);
        
        ListView<Class<? extends RequestPanel>> panelList = 
            new ListView<Class<? extends RequestPanel>>("panels", panels) {
            
            @Override
            protected void populateItem(ListItem<Class<? extends RequestPanel>> item) {
                Class<? extends RequestPanel> clazz = item.getModel().getObject();
                try {
                    RequestPanel panel = clazz.getConstructor(String.class, RequestData.class)
                        .newInstance("content", request);
                    item.add(panel);
                    
                    item.add(new Label("label", new ResourceModel(panel.getLabelKey())));
                } 
                catch (Exception e) {
                    throw new WicketRuntimeException(e);
                }
                
            }
        };
        add(panelList);
    }

    static abstract class RequestPanel extends Panel {

        public RequestPanel(String id) {
            super(id);
        }
        
        public abstract String getLabelKey();
    }

    static class OriginPanel extends RequestPanel {

        public OriginPanel(String id, RequestData data) {
            super(id);
        
            add(new Label("ip", new PropertyModel<RequestData>(data, "remoteAddr")));
            add(new Label("host", new PropertyModel<RequestData>(data, "remoteHost")));
            //add(new Label("user", new PropertyModel<RequestData>(data, "remoteUser")));
            add(new Label("lat", new PropertyModel<RequestData>(data, "remoteLat")));
            add(new Label("lon", new PropertyModel<RequestData>(data, "remoteLon")));
            add(new Label("country", new PropertyModel<RequestData>(data, "remoteCountry")));
            add(new Label("city", new PropertyModel<RequestData>(data, "remoteCity")));
            
            OriginMapPanel map = new OriginMapPanel("map", data);
            add(map);
//            OpenLayersMapPanel mapPanel = new OpenLayersMapPanel("map");
//            mapPanel.add(getCatalog().getLayerByName("monitor:monitor_world"));
//            
//            Layer l = mapPanel.add(getCatalog().getLayerByName("monitor:monitor_requests")); 
//            l.getParams().put("cql_filter", "request_id = '" + data.getId() + "'");
//            l.getParams().put("transparent", "true");
//            l.setBaseLayer(false);
//            
//                
//            add(mapPanel);
        }
        
        @Override
        public String getLabelKey() {
            return "origin";
        }
    }
    
    static class OriginMapPanel extends Panel implements IHeaderContributor {

        RequestData request;
        
        public OriginMapPanel(String id, RequestData request) {
            super(id);
            this.request = request;
            setOutputMarkupId(true);
        }

        public void renderHead(IHeaderResponse response) {
            try {
                //render css
                SimpleHash model = new SimpleHash();
                model.put("markupId", getMarkupId());
                response.renderString( renderTemplate("origin-ol-css.ftl", model) );
                
                //TODO: point back to GeoServer
                response.renderJavascriptReference("http://openlayers.org/api/OpenLayers.js");  
                
                model.put("origin", String.format("POINT(%f %f)", 
                    request.getRemoteLon(), request.getRemoteLat()));
                
                response.renderOnLoadJavascript(renderTemplate("origin-ol-onload.ftl", model));
            }
            catch( Exception e ) {
                throw new RuntimeException(e);
            }
        }
        
        Point reproject(Point p) {
            try {
                MathTransform tx = CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode("EPSG:3857"));
                return (Point) JTS.transform(p,tx);
            }
            catch(Exception e) {
                throw new WicketRuntimeException(e);
            }
        }
        
        String renderTemplate(String t, Object model) throws Exception {
            return Analytics.renderTemplate(model, t);
        }
    }
   
    static class QueryStringPanel extends RequestPanel {

        public QueryStringPanel(String id, RequestData data) {
            super(id);
            
            RepeatingView paramView = new RepeatingView("params");
            add(paramView);
            
            String queryString = data.getQueryString();
            if (queryString != null) {
                String[] kvps = queryString.split("&");
                for (String kvp : kvps) {
                    String[] split = kvp.split("=");
                    
                    WebMarkupContainer param = new WebMarkupContainer(paramView.newChildId());
                    param.add(new Label("key", split[0].toUpperCase()));
                    param.add(new Label("value", split.length > 1 ? split[1] : ""));
                    
                    paramView.add(param);
                }
            }
        }
     
        @Override
        public String getLabelKey() {
            return "queryString";
        }
    }
    
    static class BodyPanel extends RequestPanel {

        public BodyPanel(String id, final RequestData data) {
            super(id);
            
            add(new Label("contentType", new PropertyModel<RequestData>(data, "bodyContentType")));
            add(new Label("contentLength", new PropertyModel<RequestData>(data, "bodyContentLength")));
            add(new TextArea<RequestData>("body", new Model(handleBodyContent(data))));
            add(new AjaxLink("copy") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    copyToClipBoard(handleBodyContent(data));
                }
            });
        }
        
        public String getLabelKey() {
            return "body";
        };
        
        String handleBodyContent(RequestData request) {
            byte[] bytes = request.getBody();
            try {
                return new String(bytes, "UTF-8");
            } 
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    static class ResponsePanel extends RequestPanel {
        private final RequestData data;
        public ResponsePanel(String id, RequestData data) {
            super(id);
            this.data = data;
            add(new Label("content", new PropertyModel<RequestData>(data, "responseContentType")));
            add(new Label("length", new PropertyModel<RequestData>(data, "responseLength")));
            
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            if ("get".equalsIgnoreCase(data.getHttpMethod())) {
                String url = "/geoserver" + // @todo proxy path?
                        data.getPath() + 
                        (data.getQueryString() == null ? "" : "?" + data.getQueryString());
                String linkText = getLocalizer().getString("requestLink", getPage());
                ExternalLink link = new ExternalLink("requestLink",url,linkText);
                add(link);
            }
        }
        
        @Override
        public String getLabelKey() {
            return "response";
        }
    }
    
    static class ErrorPanel extends RequestPanel {

        public ErrorPanel(String id, final RequestData data) {
            super(id);
            
            add(new Label("message", new PropertyModel(data, "errorMessage")));
            add(new TextArea("stackTrace", new Model(handleStackTrace(data))));
            add(new AjaxLink("copy") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    copyToClipBoard(handleStackTrace(data));
                }
            });
        }
        
        public String getLabelKey() {
            return "error";
        };

        String handleStackTrace(RequestData data) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(out);
            data.getError().printStackTrace(writer);
            writer.flush();
            
            return new String(out.toByteArray());
        }
    }
    
    static void copyToClipBoard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }
}
