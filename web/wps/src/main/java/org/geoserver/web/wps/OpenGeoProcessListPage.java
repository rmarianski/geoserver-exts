/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.wps.web.WPSRequestBuilder;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;

public class OpenGeoProcessListPage extends GeoServerBasePage {
    private final ModalWindow detailsView = new ModalWindow("popup");
    
    {
        detailsView.setInitialHeight(400);
        detailsView.setInitialWidth(600);
    }
    
	public OpenGeoProcessListPage() {
		GeoServerTablePanel<ProcessDescriptor> table = 
			new GeoServerTablePanel<ProcessDescriptor>("processes", new OpenGeoProcessProvider()) {
				private static final long serialVersionUID = -9023864236488736961L;

				@Override
				protected Component getComponentForProperty(String id, IModel itemModel, Property<ProcessDescriptor> property) {
					if (property == OpenGeoProcessProvider.NAME) { 
						return new Label(id, property.getModel(itemModel));
					} else if (property == OpenGeoProcessProvider.DESCRIPTION) {
					    return new Label(id, property.getModel(itemModel));
					} else if (property == OpenGeoProcessProvider.LINKS) {
					    Fragment fragment = new Fragment(id, "links", OpenGeoProcessListPage.this);
					    fragment.add(createDetailLink("details", itemModel));
					    fragment.add(createBuilderLink("builder", itemModel));
					    return fragment;
					} else {
    					return null;
					}
				}
			
		    };
		add(detailsView, table);
	}
	
	@SuppressWarnings("serial")
    private AjaxLink<?> createDetailLink(final String id, final IModel<ProcessDescriptor> pd) {
	    return new AjaxLink<Void>(id) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                Fragment details = new Fragment(detailsView.getContentId(), "detail", OpenGeoProcessListPage.this);
                
                ProcessDescriptor desc = pd.getObject();
                ProcessFactory factory = Processors.createProcessFactory(desc.getQualifiedName());
                details.add(
                        new Label("title", factory.getTitle(desc.getQualifiedName()).toString()),
                        new Label("description", new PropertyModel<String>(pd, "description")),
                        new Label("name", desc.getName()),
                        createParameterList("input", factory.getParameterInfo(desc.getQualifiedName()).values()),
                        createParameterList("output", factory.getResultInfo(desc.getQualifiedName(), Collections.<String, Object>emptyMap()).values()));
                
                detailsView.setContent(details);
                detailsView.show(target);
            }
        };
	}
	
	private static final Link<Void> createBuilderLink(String id, IModel<ProcessDescriptor> model) {
	    ProcessDescriptor pd = model.getObject();
	    PageParameters parameters = new PageParameters();
	    parameters.add(WPSRequestBuilder.PARAM_NAME, pd.getName());
	    return new BookmarkablePageLink<Void>(id, WPSRequestBuilder.class, parameters);
	}
	
	@SuppressWarnings("serial")
    private static ListView<ParameterInfo> createParameterList(final String id, final Collection<Parameter<?>> params) {
	    List<ParameterInfo> inputs = new ArrayList<ParameterInfo>();
	    
	    for (Parameter<?> input : params) {
	        inputs.add(new ParameterInfo(input.getTitle().toString(), input.getType().getSimpleName()));
	    }
	    
        return new ListView<ParameterInfo>(id, inputs) {
            @Override
            protected void populateItem(ListItem<ParameterInfo> item) {
                item.add(
                        new Label("name", new PropertyModel<String>(item.getModel(), "title")),
                        new Label("type", new PropertyModel<Class<?>>(item.getModel(), "type")));
            }
        };
	}
	
	@SuppressWarnings("serial")
    private static class ParameterInfo implements Serializable {
	    private final String title;
	    private final String type;
	    
	    public ParameterInfo(String title, String type) {
	        this.title = title;
	        this.type = type;
	    }
	    
	    @SuppressWarnings("unused")
        public String getTitle() { return title; }
	    
	    @SuppressWarnings("unused")
        public String getType() { return type; }
	}
}