/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;

/**
 * Vaadin component implementing support for {@link AttributeValueSyntax} implementation.
 * Allows to render attribute value and to provide an edit panel.
 * @author K. Benedyczak
 */
public interface WebAttributeHandler
{
	public final static int MIN_VALUE_TEXT_LEN = 16;
	
	/**
	 * Defines the size of the returned representation of an attribute value
	 * @author K. Benedyczak
	 */
	public enum RepresentationSize {
		/**
		 * no restrictions on size
		 */
		ORIGINAL, 
		
		/**
		 * Smallest representation should fit into one line, table line etc
		 */
		LINE, 
		
		/**
		 * Can be bigger then one line but should fit into a regular form, typically not more then 
		 * ca 3 lines in height.
		 */
		MEDIUM
	}
	
	/**
	 * @param value
	 * @param syntax
	 * @param limited if more then zero, then the string representation should be no longer then
	 * the limit. It may be assumed that the limited won't be between 0 and MIN_VALUE_TEXT_LEN. 
	 * @return string representation, never null. For values which have no string representation some
	 * type based description should be returned as 'Jpeg image'
	 */
	public String getValueAsString(String value, int limited);
	
	/**
	 * @param value
	 * @param syntax
	 * @param size
	 * @return component allowing to present the value
	 */
	public Component getRepresentation(String value, RepresentationSize size);
	
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
