/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;

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
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.types.basic.EntityParam;

class RefreshTokenHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, RefreshTokenHandler.class);

	private final OAuthASProperties config;
	private final OAuthRefreshTokenRepository refreshTokensRepository;
	private final AccessTokenFactory accessTokenFactory;
	private final OAuthAccessTokenRepository accessTokensRepository;
	private final OAuthClientTokensCleaner tokenCleaner;
	private final TokenService tokenService;

	RefreshTokenHandler(OAuthASProperties config, OAuthRefreshTokenRepository refreshTokensDAO,
			AccessTokenFactory accessTokenFactory, OAuthAccessTokenRepository accessTokensDAO,
			OAuthClientTokensCleaner tokenCleaner, TokenService tokenService)
	{
		this.config = config;
		this.refreshTokensRepository = refreshTokensDAO;
		this.accessTokenFactory = accessTokenFactory;
		this.accessTokensRepository = accessTokensDAO;
		this.tokenCleaner = tokenCleaner;
		this.tokenService = tokenService;
	}

	Response handleRefreshTokenGrant(String refToken, String scope, String acceptHeader)
			throws EngineException, JsonProcessingException
	{
		Optional<Token> usedRefreshToken = getUsedRefreshTokenIfRotationIsActive(refToken);
		if (usedRefreshToken.isPresent())
		{
			clearTokensForClient(usedRefreshToken.get());
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "refresh token has already been used");
		}

		Token refreshToken = null;
		OAuthToken parsedRefreshToken = null;
		try
		{
			refreshToken = refreshTokensRepository.readRefreshToken(refToken);
			parsedRefreshToken = BaseOAuthResource.parseInternalToken(refreshToken);
		} catch (Exception e)
		{
			return BaseOAuthResource.makeError(OAuth2Error.INVALID_REQUEST, "wrong refresh token");
		}

		long callerEntityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		if (parsedRefreshToken.getClientId() != callerEntityId)
		{
			log.warn("Client with id {} presented use refresh code issued for client",
					parsedRefreshToken.getClientId());
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
			newToken = tokenService.prepareNewTokenBasedOnOldToken(parsedRefreshToken, scope, oldRequestedScopesList,
					refreshToken.getOwner(), callerEntityId, parsedRefreshToken.getClientUsername(), true,
					GrantType.REFRESH_TOKEN.getValue());
		} catch (OAuthErrorException e)
		{
			return e.response;
		}

		Date now = new Date();
		Date accessExpiration = TokenUtils.getAccessTokenExpiration(config, now);

		AccessToken accessToken = accessTokenFactory.create(newToken, now, acceptHeader);
		newToken.setAccessToken(accessToken.getValue());

		RefreshToken rotatedRefreshToken = refreshTokensRepository
				.rotateRefreshTokenIfNeeded(config, now, newToken, parsedRefreshToken, refreshToken.getOwner())
				.orElse(null);

		AccessTokenResponse oauthResponse = tokenService.getAccessTokenResponse(newToken, accessToken, rotatedRefreshToken,
				null);
		log.info("Refreshed access token {} of entity {}, valid until {}",
				BaseOAuthResource.tokenToLog(accessToken.getValue()), refreshToken.getOwner(), accessExpiration);
		accessTokensRepository.storeAccessToken(accessToken, newToken, new EntityParam(refreshToken.getOwner()), now,
				accessExpiration);

		return BaseOAuthResource.toResponse(Response.ok(BaseOAuthResource.getResponseContent(oauthResponse)));

	}

	private Optional<Token> getUsedRefreshTokenIfRotationIsActive(String refToken)
	{
		if (config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION))
		{
			return refreshTokensRepository.getUsedRefreshToken(refToken);
		}
		
		return Optional.empty();
	}
	
	private void clearTokensForClient(Token usedRefreshToken)
	{
		OAuthToken oldRefreshToken = OAuthToken.getInstanceFromJson(usedRefreshToken.getContents());
		tokenCleaner.removeTokensForClient(oldRefreshToken.getClientId(), usedRefreshToken.getOwner(),
				oldRefreshToken.getFirstRefreshRollingToken());
		log.warn(
				"Trying to reuse already used refresh token, revoke the currently active oauth tokens for client {} {}",
				oldRefreshToken.getClientId(), oldRefreshToken.getClientName());
		
		
	}

}
