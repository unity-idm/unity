/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.rp.local.AccessTokenAndPasswordVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

@Component
class LocalOAuthRPAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private final MessageSource msg;
	private final CredentialManagement credMan;

	@Autowired
	LocalOAuthRPAuthenticatorEditorFactory(MessageSource msg, CredentialManagement credMan)
	{
		this.msg = msg;
		this.credMan = credMan;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return AccessTokenAndPasswordVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new LocalOAuthRPAuthenticatorEditor(msg, credMan.getCredentialDefinitions());
	}

}
