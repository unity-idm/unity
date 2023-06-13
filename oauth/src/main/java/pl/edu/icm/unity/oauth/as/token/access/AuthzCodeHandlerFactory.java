/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.token.access.TokenService.TokenServiceFactory;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@Component
class AuthzCodeHandlerFactory
{
	private final TokensManagement tokensManagement;
	private final TransactionalRunner tx;
	private final OAuthAccessTokenRepository accessTokenDAO;
	private final OAuthRefreshTokenRepository refreshTokenDAO;
	private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;
	private final TokenServiceFactory tokenServiceFactory;

	@Autowired
	AuthzCodeHandlerFactory(TokensManagement tokensManagement, TransactionalRunner tx,
			OAuthAccessTokenRepository accessTokenDAO, OAuthRefreshTokenRepository refreshTokenDAO,
			OAuthTokenStatisticPublisherFactory statisticPublisherFactory, TokenServiceFactory tokenServiceFactory)
	{

		this.tokensManagement = tokensManagement;
		this.tx = tx;
		this.accessTokenDAO = accessTokenDAO;
		this.refreshTokenDAO = refreshTokenDAO;
		this.statisticPublisherFactory = statisticPublisherFactory;
		this.tokenServiceFactory = tokenServiceFactory;
	}

	AuthzCodeHandler getHandler(OAuthASProperties config, ResolvedEndpoint endpoint)
	{
		return new AuthzCodeHandler(tokensManagement, accessTokenDAO, refreshTokenDAO, tx,
				new AccessTokenFactory(config),
				statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, endpoint), config,
				tokenServiceFactory.getTokenService(config));
	}

}