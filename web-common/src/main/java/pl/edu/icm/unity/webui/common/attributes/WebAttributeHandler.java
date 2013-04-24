/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

/**
 * Vaadin component implementing support for {@link AttributeValueSyntax} implementation.
 * Allows to render attribute value and to provide an edit panel.
 * @author K. Benedyczak
 */
public interface WebAttributeHandler<T>
{
	public String getSupportedSyntaxId();
	
	/**
	 * @param value
	 * @param syntax
	 * @param limited if more then zero, then the string representation should be no longer then
	 * the limit. It may be assumed that the limited won't be between 0 and 15. 
	 * @return string representation, never null. For values which have no string representation some
	 * type based description should be returned as 'Jpeg image'
	 */
	public String getValueAsString(T value, AttributeValueSyntax<T> syntax, int limited);
	
	/**
	 * 
	 * @param value
	 * @param syntax
	 * @return image representation or null if not applicable
	 */
	public Resource getValueAsImage(T value, AttributeValueSyntax<T> syntax, int maxWidth, int maxHeight);
	
	/**
	 * @param value
	 * @param syntax
	 * @return component allowing to fully present the value
	 */
	public Component getRepresentation(T value, AttributeValueSyntax<T> syntax);
	
	/**
	 * @param initialValue value to be edited or null if value is to be created from scratch
	 * @return
	 */
	public AttributeValueEditor<T> getEditorComponent(T initialValue, AttributeValueSyntax<T> syntaxDesc);
	
	/**
	 * @param syntax
	 * @return read-only component showing the syntax settings
	 */
	public Component getSyntaxViewer(AttributeValueSyntax<T> syntax);
	
	/**
	 * @param initialValue value to be edited or null if value is to be created from scratch
	 * @return
	 */
	public AttributeSyntaxEditor<T> getSyntaxEditorComponent(AttributeValueSyntax<T> initialValue);
}
