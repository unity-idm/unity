/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common.plugins.credentials;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin23.endpoint.common.plugins.ComponentsContainer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;

import java.util.Optional;


public interface CredentialEditor
{

	ComponentsContainer getEditor(CredentialEditorContext context);
	Optional<Component> getViewer(String credentialInfo);
	String getValue() throws IllegalCredentialException;
	void setCredentialError(EngineException error);
	default boolean isCredentialCleared()
	{
		return false;
	}
}
