/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.token.access.TokenService.TokenServiceFactory;

@Component
class RefreshTokenHandlerFactory
{
	private final OAuthRefreshTokenRepository refreshTokensDAO;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final OAuthClientTokensCleaner tokenCleaner;
	private final TokenServiceFactory tokenUtilsFactory;
	private final EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter;

	@Autowired
	RefreshTokenHandlerFactory(OAuthRefreshTokenRepository refreshTokensDAO, OAuthAccessTokenRepository accessTokensDAO,
			OAuthClientTokensCleaner tokenCleaner, TokenServiceFactory tokenUtilsFactory,
			EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter)
	{
		this.refreshTokensDAO = refreshTokensDAO;
		this.accessTokensDAO = accessTokensDAO;
		this.tokenCleaner = tokenCleaner;
		this.tokenUtilsFactory = tokenUtilsFactory;
		this.oAuthTokenEffectiveScopesAttributesCompleter = oAuthTokenEffectiveScopesAttributesCompleter;
	}

	RefreshTokenHandler getHandler(OAuthASProperties config)
	{
		return new RefreshTokenHandler(config, refreshTokensDAO, new AccessTokenFactory(config), accessTokensDAO,
				tokenCleaner, tokenUtilsFactory.getTokenService(config), oAuthTokenEffectiveScopesAttributesCompleter);
	}

}