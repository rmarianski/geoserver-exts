package org.geoserver.web.wps;

import java.io.Serializable;

import org.opengis.feature.type.Name;

@SuppressWarnings("serial")
public final class ProcessDescriptor implements Serializable {
    public final Name qualifiedName;
    public final String name;
	public final String description;
	
	public ProcessDescriptor(final Name qualifiedName, final String description) {
	    this.qualifiedName = qualifiedName;
		this.name = qualifiedName.getURI();
		this.description = description;
	}
	
	public String getName() {
	    return name;
	}
	
	public Name getQualifiedName() {
	    return qualifiedName;
	}
	
	public String getDescription() {
	    return description;
	}
}
