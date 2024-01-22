/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.web;

import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.rp.verificator.BearerTokenVerificator;

@Component
class OAuthRPAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private final MessageSource msg;
	private final OAuthAccessTokenRepository tokenDAO;
	private final PKIManagement pkiMan;
	private final InputTranslationProfileFieldFactory profileFieldFactory;

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
