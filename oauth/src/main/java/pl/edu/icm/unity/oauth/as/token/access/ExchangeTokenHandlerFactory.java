/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.token.access.ClientAttributesProvider.ClientAttributesProviderFactory;
import pl.edu.icm.unity.oauth.as.token.access.TokenService.TokenServiceFactory;

@Component
class ExchangeTokenHandlerFactory
{
	private final OAuthRefreshTokenRepository refreshTokensDAO;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final TokenServiceFactory tokenServiceFactory;
	private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;
	private final ClientAttributesProviderFactory clientAttributesProviderFactory;
	private final EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter;

	@Autowired
	ExchangeTokenHandlerFactory(OAuthRefreshTokenRepository refreshTokensDAO,
			OAuthAccessTokenRepository accessTokensDAO, TokenServiceFactory tokenUtilsFactory,
			OAuthTokenStatisticPublisherFactory statisticPublisherFactory,
			ClientAttributesProviderFactory clientAttributesProviderFactory,
			EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter)
	{

		this.refreshTokensDAO = refreshTokensDAO;
		this.accessTokensDAO = accessTokensDAO;
		this.tokenServiceFactory = tokenUtilsFactory;
		this.statisticPublisherFactory = statisticPublisherFactory;
		this.clientAttributesProviderFactory = clientAttributesProviderFactory;
		this.oAuthTokenEffectiveScopesAttributesCompleter = oAuthTokenEffectiveScopesAttributesCompleter;
	}

	ExchangeTokenHandler getHandler(OAuthASProperties config, ResolvedEndpoint endpoint)
	{
		return new ExchangeTokenHandler(config, refreshTokensDAO, new AccessTokenFactory(config), accessTokensDAO,
				tokenServiceFactory.getTokenService(config),
				statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, endpoint),
				clientAttributesProviderFactory.getClientAttributeProvider(config), oAuthTokenEffectiveScopesAttributesCompleter);
	}

}