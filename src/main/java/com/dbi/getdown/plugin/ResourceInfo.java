package com.dbi.getdown.plugin;

import org.apache.maven.plugins.annotations.Parameter;

public class ResourceInfo {
	@Parameter
	private String source;

	@Parameter
	private String destination;

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}
}
