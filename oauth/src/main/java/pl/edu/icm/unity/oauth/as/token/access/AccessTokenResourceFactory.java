/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;

@Component
public class AccessTokenResourceFactory
{

	private final AuthzCodeHandlerFactory authzCodeHandlerFactory;
	private final RefreshTokenHandlerFactory refreshTokenHandlerFactory;
	private final ExchangeTokenHandlerFactory exchangeTokenHandlerFactory;
	private final CredentialFlowHandlerFactory credentialFlowHandlerFactory;
	private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;

	@Autowired
	AccessTokenResourceFactory(AuthzCodeHandlerFactory authzCodeHandlerFactory,
			RefreshTokenHandlerFactory refreshTokenHandlerFactory,
			ExchangeTokenHandlerFactory exchangeTokenHandlerFactory,
			CredentialFlowHandlerFactory credentialFlowHandlerFactory,
			OAuthTokenStatisticPublisherFactory statisticPublisherFactory)
	{

		this.authzCodeHandlerFactory = authzCodeHandlerFactory;
		this.refreshTokenHandlerFactory = refreshTokenHandlerFactory;
		this.exchangeTokenHandlerFactory = exchangeTokenHandlerFactory;
		this.credentialFlowHandlerFactory = credentialFlowHandlerFactory;
		this.statisticPublisherFactory = statisticPublisherFactory;
	}

	public AccessTokenResource getHandler(OAuthASProperties config, ResolvedEndpoint description)
	{
		return new AccessTokenResource(authzCodeHandlerFactory.getHandler(config, description),
				refreshTokenHandlerFactory.getHandler(config),
				exchangeTokenHandlerFactory.getHandler(config, description),
				credentialFlowHandlerFactory.getHandler(config, description),
				statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, description));
	}

}
