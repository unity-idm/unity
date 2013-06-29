/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Implementations allow for editing a {@link CredentialDefinition}.
 * @author K. Benedyczak
 */
public interface CredentialDefinitionEditor
{
	/**
	 * @param credentialDefinitionConfiguration current configuration to initialize the UI. Can be null,
	 * if the component is used to provide a new credential definition. 
	 * @return the editor component
	 */
	public Component getEditor(String credentialDefinitionConfiguration);
	
	/**
	 * @return the serialized state of the edited credential definition. 
	 * @throws IllegalCredentialException if the entered data is incomplete or invalid.
	 */
	public String getCredentialDefinition() throws IllegalCredentialException;

}
