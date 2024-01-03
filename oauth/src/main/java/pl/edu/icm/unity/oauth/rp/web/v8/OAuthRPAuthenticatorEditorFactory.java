/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.web.v8;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.rp.verificator.BearerTokenVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link OAuthRPAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component("OAuthRPAuthenticatorEditorFactoryV8")
class OAuthRPAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private MessageSource msg;
	private OAuthAccessTokenRepository tokenDAO;
	private PKIManagement pkiMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;;

	@Autowired
	OAuthRPAuthenticatorEditorFactory(MessageSource msg, OAuthAccessTokenRepository tokenDAO, PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory)
	{
		this.msg = msg;
		this.tokenDAO = tokenDAO;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return BearerTokenVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new OAuthRPAuthenticatorEditor(msg, tokenDAO, pkiMan, profileFieldFactory);
	}

}
