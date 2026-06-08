/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import org.springframework.stereotype.Component;

import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorFactory;
import pl.edu.icm.unity.base.message.MessageSource;

@Component
class ClientPublicKeysCredentialEditorFactory implements CredentialEditorFactory
{
	private final MessageSource msg;

	ClientPublicKeysCredentialEditorFactory(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return PrivateKeyJwtVerificator.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return new ClientPublicKeysCredentialEditor(msg);
	}

	@Override
	public CredentialDefinitionEditor creteCredentialDefinitionEditor()
	{
		return new ClientPublicKeysCredentialDefinitionEditor(msg);
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return new ClientPublicKeysCredentialDefinitionEditor(msg);
	}
}
