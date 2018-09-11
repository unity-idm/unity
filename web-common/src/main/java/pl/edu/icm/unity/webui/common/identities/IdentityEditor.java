/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.common.ComponentsContainer;

/**
 * Implementations allow to edit a value of an {@link Identity} of a fixed type.
 * @author K. Benedyczak
 */
public interface IdentityEditor
{
	/**
	 * @return the editor component
	 */
	public ComponentsContainer getEditor(IdentityEditorContext context);
	
	/**
	 * @return the current identity value
	 * @throws IllegalIdentityValueException if the entered data is incomplete or invalid.
	 */
	public IdentityParam getValue() throws IllegalIdentityValueException;
	
	/**
	 * @param value 
	 */
	public void setDefaultValue(IdentityParam value);
	
	/**
	 * Sets a label to be used by the editor instead of the default one. Used
	 * when multiple editors are displayed next to each other to distinguish them.
	 * @param value
	 */
	public void setLabel(String value);
}
