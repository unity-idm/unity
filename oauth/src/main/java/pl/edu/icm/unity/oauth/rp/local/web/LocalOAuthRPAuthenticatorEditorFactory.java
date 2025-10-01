/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local.web;

import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.oauth.rp.local.LocalAccessTokenVerificator;

@Component
class LocalOAuthRPAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private final MessageSource msg;

	@Autowired
	LocalOAuthRPAuthenticatorEditorFactory(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return LocalAccessTokenVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new LocalOAuthRPAuthenticatorEditor(msg);
	}
}
