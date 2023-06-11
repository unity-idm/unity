/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.authn.CredentialDefinition;

/**
 * Implementations allow for presenting a {@link CredentialDefinition}.
 * @author K. Benedyczak
 */
public interface CredentialDefinitionViewer
{
	/**
	 * @param credentialDefinitionConfiguration current configuration to initialize the UI. Can be null,
	 * if the component is used to provide a new credential definition. 
	 * @return the editor component
	 */
	public Component getViewer(String credentialDefinitionConfiguration);
}
