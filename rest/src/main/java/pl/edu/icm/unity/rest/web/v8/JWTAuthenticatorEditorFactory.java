/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web.v8;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.rest.jwt.authn.JWTVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link JWTAuthenticatorEditor}
 * @author P.Piernik
 *
 */
@Component("JWTAuthenticatorEditorFactoryV8")
class JWTAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private MessageSource msg;
	private PKIManagement pkiMan;

	@Autowired
	JWTAuthenticatorEditorFactory(MessageSource msg, PKIManagement pkiMan)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return JWTVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new JWTAuthenticatorEditor(msg, pkiMan.getCredentialNames());
	}
}
