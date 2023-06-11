/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Date;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.OAuthValidationException;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.ClientCredentialsProcessor;

class CredentialFlowHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, CredentialFlowHandler.class);

	private final OAuthASProperties config;
	private final ClientCredentialsProcessor clientGrantProcessor;
	private final OAuthTokenStatisticPublisher statisticPublisher;
	private final AccessTokenFactory accessTokenFactory;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final ClientAttributesProvider clientAttributesProvider;

	CredentialFlowHandler(OAuthASProperties config, ClientCredentialsProcessor clientGrantProcessor,
			OAuthTokenStatisticPublisher statisticPublisher, AccessTokenFactory accessTokenFactory,
			OAuthAccessTokenRepository accessTokensDAO, ClientAttributesProvider clientAttributesProvider)
	{
		this.config = config;
		this.clientGrantProcessor = clientGrantProcessor;
		this.statisticPublisher = statisticPublisher;
		this.accessTokenFactory = accessTokenFactory;
		this.accessTokensDAO = accessTokensDAO;
		this.clientAttributesProvider = clientAttributesProvider;
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
					clientAttributesProvider.getClientName(new EntityParam(loginSession.getEntityId())));
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, e.getMessage());
		}

		AccessToken accessToken = accessTokenFactory.create(internalToken, now, acceptHeader);
		internalToken.setAccessToken(accessToken.getValue());

		Date expiration = TokenUtils.getAccessTokenExpiration(config, now);
		log.info("Client cred grant: issuing new access token {}, valid until {}",
				BaseOAuthResource.tokenToLog(accessToken.getValue()), expiration);
		AccessTokenResponse oauthResponse = new AccessTokenResponse(new Tokens(accessToken, null));

		statisticPublisher.reportSuccess(internalToken.getClientUsername(), internalToken.getClientName(),
				new EntityParam(internalToken.getClientId()));
		accessTokensDAO.storeAccessToken(accessToken, internalToken, new EntityParam(internalToken.getClientId()), now,
				expiration);

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));
	}
}
