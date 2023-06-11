/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.token.access.ClientAttributesProvider.ClientAttributesProviderFactory;
import pl.edu.icm.unity.oauth.as.token.access.TokenService.TokenServiceFactory;

@Component
class ExchangeTokenHandlerFactory
{
	private final OAuthRefreshTokenRepository refreshTokensDAO;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final TokenServiceFactory tokenServiceFactory;
	private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;
	private final OAuthRequestValidatorFactory requestValidatorFactory;
	private final EntityManagement idMan;
	private final ClientAttributesProviderFactory clientAttributesProviderFactory;

	@Autowired
	ExchangeTokenHandlerFactory(OAuthRefreshTokenRepository refreshTokensDAO,
			OAuthAccessTokenRepository accessTokensDAO, TokenServiceFactory tokenUtilsFactory,
			OAuthTokenStatisticPublisherFactory statisticPublisherFactory,
			OAuthRequestValidatorFactory requestValidatorFactory, EntityManagement idMan,
			ClientAttributesProviderFactory clientAttributesProviderFactory)
	{

		this.refreshTokensDAO = refreshTokensDAO;
		this.accessTokensDAO = accessTokensDAO;
		this.tokenServiceFactory = tokenUtilsFactory;
		this.statisticPublisherFactory = statisticPublisherFactory;
		this.requestValidatorFactory = requestValidatorFactory;
		this.idMan = idMan;
		this.clientAttributesProviderFactory = clientAttributesProviderFactory;
	}

	ExchangeTokenHandler getHandler(OAuthASProperties config, ResolvedEndpoint endpoint)
	{
		return new ExchangeTokenHandler(config, refreshTokensDAO, new AccessTokenFactory(config), accessTokensDAO,
				tokenServiceFactory.getTokenService(config),
				statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, endpoint),
				requestValidatorFactory.getOAuthRequestValidator(config), idMan,
				clientAttributesProviderFactory.getClientAttributeProvider(config));
	}

}