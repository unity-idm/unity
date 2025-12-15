/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.token.access.ClientAttributesProvider.ClientAttributesProviderFactory;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthIdPEngine;

@Component
class ExchangeTokenHandlerFactory
{
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;
	private final ClientAttributesProviderFactory clientAttributesProviderFactory;
	private final EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter;
	private final EntityManagement idMan;
	private final IdPEngine notAuthorizedOauthIdpEngine;

	@Autowired
	ExchangeTokenHandlerFactory(OAuthAccessTokenRepository accessTokensDAO,
			OAuthTokenStatisticPublisherFactory statisticPublisherFactory,
			ClientAttributesProviderFactory clientAttributesProviderFactory,
			EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter,
			@Qualifier("insecure") EntityManagement idMan,
			@Qualifier("insecure") IdPEngine notAuthorizedOauthIdpEngine)
	{

		this.accessTokensDAO = accessTokensDAO;
		this.statisticPublisherFactory = statisticPublisherFactory;
		this.clientAttributesProviderFactory = clientAttributesProviderFactory;
		this.oAuthTokenEffectiveScopesAttributesCompleter = oAuthTokenEffectiveScopesAttributesCompleter;
		this.idMan = idMan;
		this.notAuthorizedOauthIdpEngine = notAuthorizedOauthIdpEngine;
	}

	ExchangeTokenHandler getHandler(OAuthASProperties config, ResolvedEndpoint endpoint)
	{
		return new ExchangeTokenHandler(config, new AccessTokenFactory(config), accessTokensDAO,
				statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, endpoint),
				clientAttributesProviderFactory.getClientAttributeProvider(config),
				oAuthTokenEffectiveScopesAttributesCompleter,
				idMan, new OAuthIdPEngine(notAuthorizedOauthIdpEngine));
	}

}