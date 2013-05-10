/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;

/**
 * Implementations allow to edit a value of a credential of a fixed type.
 * @author K. Benedyczak
 */
public interface CredentialEditor
{
	/**
	 * @return the editor component
	 */
	public Component getEditor(String credentialConfiguration);
	
	/**
	 * @return the credential value
	 * @throws IllegalCredentialException if the entered data is incomplete or invalid.
	 */
	public String getValue() throws IllegalCredentialException;
}
