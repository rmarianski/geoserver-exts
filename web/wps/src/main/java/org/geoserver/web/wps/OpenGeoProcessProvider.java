package org.geoserver.web.wps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

public final class OpenGeoProcessProvider extends GeoServerDataProvider<ProcessDescriptor> {

	private static final long serialVersionUID = -4209175487620050606L;

        private static final Logger LOGGER = Logging.getLogger(OpenGeoProcessProvider.class);
	
	public static final Property<ProcessDescriptor> NAME = new BeanProperty<ProcessDescriptor>("name", "name");
	public static final Property<ProcessDescriptor> DESCRIPTION = new BeanProperty<ProcessDescriptor>("description", "description");
	public static final Property<ProcessDescriptor> LINKS = new PropertyPlaceholder<ProcessDescriptor>("links");

	@SuppressWarnings("unchecked")
	@Override
	protected List<Property<ProcessDescriptor>> getProperties() {
		return Arrays.asList(NAME, DESCRIPTION, LINKS);
	}

	@Override
	protected List<ProcessDescriptor> getItems() {
	    List<ProcessDescriptor> results = new ArrayList<ProcessDescriptor>();
	    for (ProcessFactory factory : Processors.getProcessFactories()) {
	        for (Name name : factory.getNames()) {
                    try {
	                results.add(new ProcessDescriptor(name, factory.getDescription(name).toString()));
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.FINE, "Error getting description for process: " + name, e);
                    }
	        }
	    }
	    
            return results;
	}
}
