/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials;

import com.vaadin.flow.component.Component;

import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;

import java.util.Optional;


public interface CredentialEditor
{
	ComponentsContainer getEditor(CredentialEditorContext context);
	Optional<Component> getViewer(String credentialInfo);
	String getValue() throws IllegalCredentialException;
	void setCredentialError(EngineException error);
	
	default boolean isUserConfigurable() 
	{
		return true;
	}
	
	default boolean isCredentialCleared()
	{
		return false;
	}
}
