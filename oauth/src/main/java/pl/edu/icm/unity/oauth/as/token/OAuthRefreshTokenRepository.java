/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.store.api.TokenDAO.TokenNotFoundException;
import pl.edu.icm.unity.oauth.as.OAuthSystemScopeProvider;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Built on top of generic token storage handles access to persisted OAuth
 * tokens.
 */
@Component
public class OAuthRefreshTokenRepository
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthRefreshTokenRepository.class);

	public static final String INTERNAL_REFRESH_TOKEN = "oauth2Refresh";
	static final String INTERNAL_USED_REFRESH_TOKEN = "usedOauth2Refresh";

	private final TokensManagement tokensMan;
	private final SecuredTokensManagement securedTokensManagement;

	@Autowired
	public OAuthRefreshTokenRepository(TokensManagement tokensMan, SecuredTokensManagement securedTokensManagement)
	{
		this.tokensMan = tokensMan;
		this.securedTokensManagement = securedTokensManagement;
	}

	public List<Token> getAllRefreshTokens() throws EngineException
	{
		return securedTokensManagement.getAllTokens(INTERNAL_REFRESH_TOKEN);
	}

	public List<Token> getOwnedRefreshTokens() throws EngineException
	{
		return securedTokensManagement.getOwnedTokens(INTERNAL_REFRESH_TOKEN);
	}

	public Token readRefreshToken(String tokenValue)
	{
		return tokensMan.getTokenById(INTERNAL_REFRESH_TOKEN, tokenValue);
	}

	public Optional<RefreshToken> getRefreshToken(OAuthASProperties config, Date now, OAuthToken newToken, Long owner,
			Optional<String> oldRefreshToken) throws EngineException, JsonProcessingException
	{
		if (newToken.getClientType().equals(ClientType.PUBLIC)
				&& !config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION))
			return Optional.empty();
		
		RefreshToken refreshToken = checkPolicyAndGetRefreshToken(config,
				Arrays.asList(newToken.getEffectiveScope()).contains(OAuthSystemScopeProvider.OFFLINE_ACCESS_SCOPE));
		if (refreshToken != null)
		{
			newToken.setRefreshToken(refreshToken.getValue());
			oldRefreshToken.ifPresentOrElse(t -> newToken.setFirstRefreshRollingToken(t),
					() -> newToken.setFirstRefreshRollingToken(refreshToken.getValue()));
			Date refreshExpiration = getRefreshTokenExpiration(config, now);
			log.info("Issuing new refresh token {}, valid until {}",
					BaseOAuthResource.tokenToLog(refreshToken.getValue()), refreshExpiration);

			tokensMan.addToken(INTERNAL_REFRESH_TOKEN, refreshToken.getValue(), new EntityParam(owner),
					newToken.getSerialized(), now, refreshExpiration);
		}
		return Optional.ofNullable(refreshToken);
	}

	public Optional<RefreshToken> rollRefreshTokenIfNeeded(OAuthASProperties config, Date now, OAuthToken newToken,
			OAuthToken oldRefreshToken, Long owner) throws EngineException, JsonProcessingException
	{
		if (config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION)
				&& newToken.getClientType().equals(ClientType.PUBLIC))
		{
			log.info("Move refresh token {} to history", oldRefreshToken.getRefreshToken());
			tokensMan.removeToken(INTERNAL_REFRESH_TOKEN, oldRefreshToken.getRefreshToken());
			tokensMan.addToken(INTERNAL_USED_REFRESH_TOKEN, oldRefreshToken.getRefreshToken(), new EntityParam(owner),
					oldRefreshToken.getSerialized(), now, null);
			return getRefreshToken(config, now, newToken, owner,
					Optional.of(oldRefreshToken.getFirstRefreshRollingToken()));
		}

		return Optional.empty();
	}

	private Date getRefreshTokenExpiration(OAuthASProperties config, Date now)
	{
		int refreshTokenValidity = config.getRefreshTokenValidity();
		Calendar cl = Calendar.getInstance();
		cl.setTime(now);
		if (refreshTokenValidity == 0)
		{
			return null;
		} else if (refreshTokenValidity > 0)
		{
			cl.add(Calendar.SECOND, refreshTokenValidity);
		}
		return cl.getTime();
	}

	private RefreshToken checkPolicyAndGetRefreshToken(OAuthASProperties config, boolean offlineAccessRequested)
	{
		RefreshToken refreshToken = null;
		if (config.getRefreshTokenIssuePolicy().equals(RefreshTokenIssuePolicy.ALWAYS)
				|| (config.getRefreshTokenIssuePolicy().equals(RefreshTokenIssuePolicy.OFFLINE_SCOPE_BASED)
						&& offlineAccessRequested))
		{
			refreshToken = new RefreshToken();
		}
		return refreshToken;
	}

	static Date getAccessTokenExpiration(OAuthASProperties config, Date now)
	{
		int accessTokenValidity = config.getAccessTokenValidity();

		return new Date(now.getTime() + accessTokenValidity * 1000);
	}

	public void removeRefreshToken(String token, OAuthToken parsedToken, long userId)
	{
		tokensMan.removeToken(INTERNAL_REFRESH_TOKEN, token);
		try
		{
			clearHistoryForClient(parsedToken.getFirstRefreshRollingToken(), parsedToken.getClientId(), userId);
		} catch (EngineException e)
		{
			log.error("Can not remove refresh token history", e);
		}
	}

	public static boolean isRefreshToken(Token token)
	{
		return token.getType().equals(INTERNAL_REFRESH_TOKEN);
	}

	public void secureRemove(String value) throws EngineException
	{
		securedTokensManagement.removeToken(INTERNAL_REFRESH_TOKEN, value);
	}

	public void clearHistoryForClient(String historyId, long clientId, long userId) throws EngineException
	{
		List<Token> usedTokens = tokensMan.getOwnedTokens(INTERNAL_USED_REFRESH_TOKEN, new EntityParam(userId));
		for (Token token : usedTokens)
		{
			OAuthToken oauthToken = OAuthToken.getInstanceFromJson(token.getContents());
			if (oauthToken.getClientId() == clientId && oauthToken.getFirstRefreshRollingToken().equals(historyId))
			{
				tokensMan.removeToken(token.getType(), token.getValue());
			}
		}
	}

	public Optional<Token> getUsedRefreshToken(String refToken)
	{
		try
		{
			return Optional.of(tokensMan.getTokenById(INTERNAL_USED_REFRESH_TOKEN, refToken));
		} catch (TokenNotFoundException e)
		{
			return Optional.empty();
		}
	}
}
