/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials;

import com.vaadin.flow.component.Component;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;


public interface CredentialDefinitionEditor
{
	Component getEditor(String credentialDefinitionConfiguration);

	String getCredentialDefinition() throws IllegalCredentialException;
}
