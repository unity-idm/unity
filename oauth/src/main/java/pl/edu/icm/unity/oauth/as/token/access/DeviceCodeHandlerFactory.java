/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.token.access.TokenService.TokenServiceFactory;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@Component
class DeviceCodeHandlerFactory
{
	private final DeviceCodeRepository deviceCodeRepository;
	private final TransactionalRunner tx;
	private final OAuthAccessTokenRepository accessTokenDAO;
	private final OAuthRefreshTokenRepository refreshTokenDAO;
	private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;
	private final TokenServiceFactory tokenServiceFactory;

	@Autowired
	DeviceCodeHandlerFactory(DeviceCodeRepository deviceCodeRepository, TransactionalRunner tx,
			OAuthAccessTokenRepository accessTokenDAO, OAuthRefreshTokenRepository refreshTokenDAO,
			OAuthTokenStatisticPublisherFactory statisticPublisherFactory, TokenServiceFactory tokenServiceFactory)
	{
		this.deviceCodeRepository = deviceCodeRepository;
		this.tx = tx;
		this.accessTokenDAO = accessTokenDAO;
		this.refreshTokenDAO = refreshTokenDAO;
		this.statisticPublisherFactory = statisticPublisherFactory;
		this.tokenServiceFactory = tokenServiceFactory;
	}

	DeviceCodeHandler getHandler(OAuthASProperties config, ResolvedEndpoint endpoint)
	{
		return new DeviceCodeHandler(deviceCodeRepository, tx, new AccessTokenFactory(config), accessTokenDAO,
				refreshTokenDAO, statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, endpoint), config,
				tokenServiceFactory.getTokenService(config));
	}
}
