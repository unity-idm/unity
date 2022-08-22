/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import java.util.Date;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenStatisticPublisher.OAuthTokenStatisticPublisherFactory;
import pl.edu.icm.unity.oauth.as.token.TokenUtils.TokenUtilsFactory;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

class CredentialFlowHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, CredentialFlowHandler.class);

	private final OAuthASProperties config;
	private final ClientCredentialsProcessor clientGrantProcessor;
	private final OAuthTokenStatisticPublisher statisticPublisher;
	private final AccessTokenFactory accessTokenFactory;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final TokenUtils tokenUtils;

	CredentialFlowHandler(OAuthASProperties config, ClientCredentialsProcessor clientGrantProcessor,
			OAuthTokenStatisticPublisher statisticPublisher, AccessTokenFactory accessTokenFactory,
			OAuthAccessTokenRepository accessTokensDAO, TokenUtils tokenUtils)
	{
		this.config = config;
		this.clientGrantProcessor = clientGrantProcessor;
		this.statisticPublisher = statisticPublisher;
		this.accessTokenFactory = accessTokenFactory;
		this.accessTokensDAO = accessTokensDAO;
		this.tokenUtils = tokenUtils;
	}

	Response handleClientCredentialFlow(String scope, String acceptHeader)
			throws EngineException, JsonProcessingException
	{
		Date now = new Date();
		OAuthToken internalToken;
		try
		{
			internalToken = clientGrantProcessor.processClientFlowRequest(scope);
		} catch (OAuthValidationException e)
		{
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			String client = loginSession.getAuthenticatedIdentities().iterator().next();
			statisticPublisher.reportFail(client,
					tokenUtils.getClientName(new EntityParam(loginSession.getEntityId())));
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, e.getMessage());
		}

		AccessToken accessToken = accessTokenFactory.create(internalToken, now, acceptHeader);
		internalToken.setAccessToken(accessToken.getValue());

		Date expiration = tokenUtils.getAccessTokenExpiration(config, now);
		log.info("Client cred grant: issuing new access token {}, valid until {}",
				BaseOAuthResource.tokenToLog(accessToken.getValue()), expiration);
		AccessTokenResponse oauthResponse = new AccessTokenResponse(new Tokens(accessToken, null));

		statisticPublisher.reportSuccess(internalToken.getClientUsername(), internalToken.getClientName(),
				new EntityParam(internalToken.getClientId()));
		accessTokensDAO.storeAccessToken(accessToken, internalToken, new EntityParam(internalToken.getClientId()), now,
				expiration);

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));
	}

	@Component
	static class CredentialFlowHandlerFactory
	{
		private final IdPEngine idpEngine;
		private final OAuthRequestValidatorFactory oauthRequestValidatorFactory;
		private final OAuthTokenStatisticPublisherFactory statisticPublisherFactory;
		private final OAuthAccessTokenRepository accessTokensDAO;
		private final TokenUtilsFactory tokenUtilsFactory;

		@Autowired
		CredentialFlowHandlerFactory(@Qualifier("insecure") IdPEngine idpEngine,
				OAuthRequestValidatorFactory oauthRequestValidatorFactory,
				OAuthTokenStatisticPublisherFactory statisticPublisher, OAuthAccessTokenRepository accessTokensDAO,
				TokenUtilsFactory tokenUtilsFactory)
		{
			this.idpEngine = idpEngine;
			this.oauthRequestValidatorFactory = oauthRequestValidatorFactory;
			this.statisticPublisherFactory = statisticPublisher;
			this.accessTokensDAO = accessTokensDAO;
			this.tokenUtilsFactory = tokenUtilsFactory;
		}

		CredentialFlowHandler getHandler(OAuthASProperties config, ResolvedEndpoint endpoint)
		{

			return new CredentialFlowHandler(config,
					new ClientCredentialsProcessor(oauthRequestValidatorFactory.getOAuthRequestValidator(config),
							idpEngine, config),
					statisticPublisherFactory.getOAuthTokenStatisticPublisher(config, endpoint),
					new AccessTokenFactory(config), accessTokensDAO, tokenUtilsFactory.getTokenUtils(config));
		}

	}
}
