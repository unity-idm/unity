/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.metadata;

import com.vaadin.flow.component.Component;

import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Editor of metadata handled by {@link AttributeMetadataProvider}
 * @author K. Benedyczak
 */
public interface AttributeMetadataEditor
{
	public Component getEditor();
	
	/**
	 * @return the edited value 
	 * @throws FormValidationException if the state of the editor component is invalid
	 */
	public String getValue() throws FormValidationException;
}
