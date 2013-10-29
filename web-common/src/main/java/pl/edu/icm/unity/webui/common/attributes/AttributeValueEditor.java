/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

import com.vaadin.ui.Component;

/**
 * Attribute value editor, with possibility to retrieve the edited value.
 * @author K. Benedyczak
 */
public interface AttributeValueEditor<T>
{
	
	public Component getEditor();
	
	/**
	 * @return the edited value 
	 * @throws IllegalAttributeValueException if the current state of the editor is invalid
	 */
	public T getCurrentValue() throws IllegalAttributeValueException;
	
	/**
	 * Updates the editor's label of the value, which was initially set upon object construction
	 * (see {@link WebAttributeHandler#getEditorComponent(Object, String, pl.edu.icm.unity.types.basic.AttributeValueSyntax)}).
	 * @param label
	 */
	public void setLabel(String label);
}
