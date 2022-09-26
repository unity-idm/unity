/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.token.ClientCredentialsProcessor;
import pl.edu.icm.unity.oauth.as.token.access.ClientAttributesProvider.ClientAttributesProviderFactory;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

@Component
class CredentialFlowHandlerFactory
{
	private final IdPEngine idpEngine;
	private final OAuthRequestValidatorFactory oauthRequestValidatorFactory;
	private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final ClientAttributesProviderFactory clientAttributesProviderFactory;

	@Autowired
	CredentialFlowHandlerFactory(@Qualifier("insecure") IdPEngine idpEngine,
			OAuthRequestValidatorFactory oauthRequestValidatorFactory,
			OAuthTokenStatisticPublisherFactory statisticPublisher, OAuthAccessTokenRepository accessTokensDAO,
			ClientAttributesProviderFactory clientAttributesProviderFactory)
	{
		this.idpEngine = idpEngine;
		this.oauthRequestValidatorFactory = oauthRequestValidatorFactory;
		this.statisticPublisherFactory = statisticPublisher;
		this.accessTokensDAO = accessTokensDAO;
		this.clientAttributesProviderFactory = clientAttributesProviderFactory;
	}

	CredentialFlowHandler getHandler(OAuthASProperties config, ResolvedEndpoint endpoint)
	{

		return new CredentialFlowHandler(config,
				new ClientCredentialsProcessor(oauthRequestValidatorFactory.getOAuthRequestValidator(config), idpEngine,
						config),
				statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, endpoint),
				new AccessTokenFactory(config), accessTokensDAO,
				clientAttributesProviderFactory.getClientAttributeProvider(config));
	}

}