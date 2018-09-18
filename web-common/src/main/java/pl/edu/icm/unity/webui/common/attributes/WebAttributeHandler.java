/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

/**
 * Vaadin component implementing support for {@link AttributeValueSyntax} implementation.
 * Allows to render attribute value and to provide an edit panel.
 * @author K. Benedyczak
 */
public interface WebAttributeHandler
{
	/**
	 * @param value
	 * @param syntax
	 * @return string representation, never null. For values which have no string representation some
	 * type based description should be returned as 'Jpeg image'
	 */
	public String getValueAsString(String value);
	
	/**
	 * @param value
	 * @return component allowing to present the value
	 */
	public Component getRepresentation(String value, AttributeViewerContext context);
	
	/**
	 * @param initialValue value to be edited or null if value is to be created from scratch
	 * @return
	 */
	public AttributeValueEditor getEditorComponent(String initialValue, String label);
	
	/**
	 * @param syntax
	 * @return read-only component showing the syntax settings
	 */
	public Component getSyntaxViewer();
}
