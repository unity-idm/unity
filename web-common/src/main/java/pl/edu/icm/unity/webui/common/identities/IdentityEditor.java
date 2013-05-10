/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Implementations allow to edit a value of an {@link Identity} of a fixed type.
 * @author K. Benedyczak
 */
public interface IdentityEditor
{
	/**
	 * @return the editor component
	 */
	public Component getEditor();
	
	/**
	 * @return the current identity value
	 * @throws IllegalIdentityValueException if the entered data is incomplete or invalid.
	 */
	public String getValue() throws IllegalIdentityValueException;
}
