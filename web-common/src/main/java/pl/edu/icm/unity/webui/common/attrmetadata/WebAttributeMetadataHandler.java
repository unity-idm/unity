/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attrmetadata;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.server.attributes.AttributeMetadataProvider;

/**
 * Allows for displaying and editing metadata of a {@link AttributeMetadataProvider}.
 * 
 * @author K. Benedyczak
 */
public interface WebAttributeMetadataHandler
{
	public String getSupportedMetadata();
	
	/**
	 * @param value
	 * @return component allowing to fully present the metadata value
	 */
	public Component getRepresentation(String value);
	
	/**
	 * @param initialValue value to be edited or null if value is to be created from scratch
	 * @return
	 */
	public AttributeMetadataEditor getEditorComponent(String initialValue);
}
