/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes;


import com.vaadin.flow.component.Component;

public interface WebAttributeHandler
{
	String getValueAsString(String value);
	
	Component getRepresentation(String value, AttributeViewerContext context);
	
	AttributeValueEditor getEditorComponent(String initialValue, String label);
	
	Component getSyntaxViewer();
}