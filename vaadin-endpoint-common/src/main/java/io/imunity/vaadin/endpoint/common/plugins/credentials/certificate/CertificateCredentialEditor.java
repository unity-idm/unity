/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.certificate;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;

import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;

public class CertificateCredentialEditor implements CredentialEditor
{
	private final MessageSource msg;
	
	CertificateCredentialEditor(MessageSource msg)
	{
		this.msg = msg;
	}

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
		return Optional.of(new Span(msg.getMessage("CertificateCredentialEditor.info")));
	}

	@Override
	public void setCredentialError(EngineException message)
	{
	}
	
	@Override
	public boolean isUserConfigurable() 
	{
		return false;
	}
}
