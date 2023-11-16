/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
class OTPCredentialEditorFactory implements CredentialEditorFactory
{
	private final ObjectFactory<OTPCredentialDefinitionEditor> credDefEditorFactory;
	private final ObjectFactory<CredentialDefinitionViewer> credDefViewerFactory;
	private final ObjectFactory<CredentialEditor> credentialEditorFactory;

	@Autowired
	public OTPCredentialEditorFactory(
			ObjectFactory<OTPCredentialDefinitionEditor> credDefEditorFactory,
			ObjectFactory<CredentialDefinitionViewer> credDefViewerFactory,
			ObjectFactory<CredentialEditor> credentialEditorFactory)
	{
		this.credDefEditorFactory = credDefEditorFactory;
		this.credDefViewerFactory = credDefViewerFactory;
		this.credentialEditorFactory = credentialEditorFactory;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return OTP.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return credentialEditorFactory.getObject();
	}

	@Override
	public CredentialDefinitionEditor creteCredentialDefinitionEditor()
	{
		return credDefEditorFactory.getObject();
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return credDefViewerFactory.getObject();
	}
}
