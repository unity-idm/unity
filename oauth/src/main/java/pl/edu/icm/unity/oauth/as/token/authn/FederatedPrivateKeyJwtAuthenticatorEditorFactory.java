/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import pl.edu.icm.unity.base.message.MessageSource;

@Component
class FederatedPrivateKeyJwtAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private final MessageSource msg;

	@Autowired
	FederatedPrivateKeyJwtAuthenticatorEditorFactory(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return FederatedPrivateKeyJwtVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance()
	{
		return new FederatedPrivateKeyJwtAuthenticatorEditor(msg);
	}
}
