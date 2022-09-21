/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.TokenUtils.TokenUtilsFactory;
import pl.edu.icm.unity.types.basic.EntityParam;

class RefreshTokenHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, RefreshTokenHandler.class);

	private final OAuthASProperties config;
	private final OAuthRefreshTokenRepository refreshTokensDAO;
	private final AccessTokenFactory accessTokenFactory;
	private final OAuthAccessTokenRepository accessTokensDAO;
	private final TokenUtils tokenUtils;

	RefreshTokenHandler(OAuthASProperties config, OAuthRefreshTokenRepository refreshTokensDAO,
			AccessTokenFactory accessTokenFactory, OAuthAccessTokenRepository accessTokensDAO, TokenUtils tokenUtils)
	{

		this.config = config;
		this.refreshTokensDAO = refreshTokensDAO;
		this.accessTokenFactory = accessTokenFactory;
		this.accessTokensDAO = accessTokensDAO;
		this.tokenUtils = tokenUtils;
	}

	Response handleRefreshToken(String refToken, String scope, String acceptHeader)
			throws EngineException, JsonProcessingException
	{
		Token refreshToken = null;
		OAuthToken parsedRefreshToken = null;
		try
		{
			refreshToken = refreshTokensDAO.readRefreshToken(refToken);
			parsedRefreshToken = BaseOAuthResource.parseInternalToken(refreshToken);
		} catch (Exception e)
		{
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "wrong refresh token");
		}

		long callerEntityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		if (parsedRefreshToken.getClientId() != callerEntityId)
		{
			log.warn("Client with id " + callerEntityId + " presented use refresh code issued " + "for client "
					+ parsedRefreshToken.getClientId());
			// intended - we mask the reason
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_GRANT, "wrong refresh token");
		}

		List<String> oldRequestedScopesList = Arrays.asList(parsedRefreshToken.getRequestedScope());
		OAuthToken newToken = null;

		// When no scopes are requested RFC mandates to assign all originally assigned
		if (scope == null)
			scope = String.join(" ", oldRequestedScopesList);

		try
		{
			newToken = tokenUtils.prepareNewToken(parsedRefreshToken, scope, oldRequestedScopesList,
					refreshToken.getOwner(), callerEntityId, parsedRefreshToken.getClientUsername(), true,
					GrantType.REFRESH_TOKEN.getValue());
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		Date now = new Date();
		Date accessExpiration = tokenUtils.getAccessTokenExpiration(config, now);

		AccessToken accessToken = accessTokenFactory.create(newToken, now, acceptHeader);
		newToken.setAccessToken(accessToken.getValue());

		RefreshToken rolledRefreshToken = refreshTokensDAO
				.getRefreshToken(config, now, newToken, refreshToken.getOwner())
				.orElse(null);

		AccessTokenResponse oauthResponse = tokenUtils.getAccessTokenResponse(newToken, accessToken, rolledRefreshToken,
				null);
		log.info("Refreshed access token {} of entity {}, valid until {}",
				BaseOAuthResource.tokenToLog(accessToken.getValue()), refreshToken.getOwner(), accessExpiration);
		accessTokensDAO.storeAccessToken(accessToken, newToken, new EntityParam(refreshToken.getOwner()), now,
				accessExpiration);

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));

	}

	@Component
	static class RefreshTokenHandlerFactory
	{
		private final OAuthRefreshTokenRepository refreshTokensDAO;
		private final OAuthAccessTokenRepository accessTokensDAO;
		private final TokenUtilsFactory tokenUtilsFactory;

		@Autowired
		RefreshTokenHandlerFactory(OAuthRefreshTokenRepository refreshTokensDAO,
				OAuthAccessTokenRepository accessTokensDAO,
				TokenUtilsFactory tokenUtilsFactory)
		{
			this.refreshTokensDAO = refreshTokensDAO;
			this.accessTokensDAO = accessTokensDAO;
			this.tokenUtilsFactory = tokenUtilsFactory;
		}

		RefreshTokenHandler getHandler(OAuthASProperties config)
		{
			return new RefreshTokenHandler(config, refreshTokensDAO, new AccessTokenFactory(config), accessTokensDAO,
					tokenUtilsFactory.getTokenUtils(config));
		}

	}

}
