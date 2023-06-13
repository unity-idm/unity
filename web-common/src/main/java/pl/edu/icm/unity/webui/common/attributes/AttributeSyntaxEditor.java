/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;

/**
 * Attribute type editor, with possibility to retrieve the edited type.
 * @author K. Benedyczak
 */
public interface AttributeSyntaxEditor<T>
{
	public Component getEditor();
	
	/**
	 * @return the edited value 
	 * @throws IllegalAttributeTypeException if the current state of the editor is invalid
	 */
	public AttributeValueSyntax<T> getCurrentValue() throws IllegalAttributeTypeException;
}
