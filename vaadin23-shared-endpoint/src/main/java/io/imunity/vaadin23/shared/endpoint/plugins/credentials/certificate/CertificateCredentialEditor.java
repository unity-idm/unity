/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.credentials.certificate;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;

import java.util.Optional;

public class CertificateCredentialEditor implements CredentialEditor
{
	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		return new ComponentsContainer();
	}

	@Override
	public String getValue() throws IllegalCredentialException
	{
		return "";
	}

	@Override
	public Optional<Component> getViewer(String credentialConfiguration)
	{
		return Optional.empty();
	}

	@Override
	public void setCredentialError(EngineException message)
	{
	}
}
