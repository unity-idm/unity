/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.BearerJWTAccessToken;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Built on top of generic token storage handles access to persisted OAuth tokens.
 */
@Component
public class OAuthAccessTokenRepository
{
	public static final String INTERNAL_ACCESS_TOKEN = "oauth2Access";
	private final TokensManagement tokensMan;
	private final SecuredTokensManagement securedTokensManagement;
	
	@Autowired
	public OAuthAccessTokenRepository(TokensManagement tokensMan, SecuredTokensManagement securedTokensManagement)
	{
		this.tokensMan = tokensMan;
		this.securedTokensManagement = securedTokensManagement;
	}

	public void storeAccessToken(AccessToken accessToken, OAuthToken internalToken, EntityParam owner, 
			Date now, Date expiration) throws IllegalIdentityValueException, IllegalTypeException, JsonProcessingException
	{
		tokensMan.addToken(INTERNAL_ACCESS_TOKEN, getTokenUniqueKey(accessToken), 
				owner, internalToken.getSerialized(), now, expiration);
	}
	
	public void updateAccessTokenExpiration(Token rawToken, Date newExpiryDate)
	{
		if (!INTERNAL_ACCESS_TOKEN.equals(rawToken.getType()))
			throw new IllegalArgumentException("Only access token can be updated with this method");
		tokensMan.updateToken(INTERNAL_ACCESS_TOKEN, rawToken.getValue(), 
				newExpiryDate, 
				rawToken.getContents());
	}
	
	public Token readAccessToken(String tokenValue)
	{
		return tokensMan.getTokenById(INTERNAL_ACCESS_TOKEN, extractTokenKey(tokenValue));
	}
	
	private String extractTokenKey(String tokenValue)
	{
		return tryGetJWTID(tokenValue).orElse(tokenValue);
	}
	
	private Optional<String> tryGetJWTID(String tokenValue)
	{
		return BearerJWTAccessToken.tryParseJWTClaimSet(tokenValue).map(claimSet -> claimSet.getJWTID());
	}

	public void removeAccessToken(String tokenKey)
	{
		tokensMan.removeToken(INTERNAL_ACCESS_TOKEN, tokenKey);
	}
	
	public List<Token> getAllAccessTokens() throws EngineException
	{
		return securedTokensManagement.getAllTokens(INTERNAL_ACCESS_TOKEN);
	}

	public List<Token> getOwnedAccessTokens() throws EngineException
	{
		return securedTokensManagement.getOwnedTokens(INTERNAL_ACCESS_TOKEN);
	}

	
	private static String getTokenUniqueKey(AccessToken token)
	{
		if (token instanceof BearerJWTAccessToken)
		{
			return ((BearerJWTAccessToken)token).getClaimsSet().getJWTID();
		} else
		{
			return token.getValue();
		}
	}

	public void removeWithAuthorization(String value) throws EngineException
	{
		securedTokensManagement.removeToken(INTERNAL_ACCESS_TOKEN, value);
		
	}

	public void removeOwnedByClient(long clientId, long userId) throws EngineException
	{
		List<Token> ownedTokens = tokensMan.getOwnedTokens(INTERNAL_ACCESS_TOKEN, new EntityParam(userId));
		for (Token token : ownedTokens)
		{
			OAuthToken oauthToken = OAuthToken.getInstanceFromJson(token.getContents());
			if (oauthToken.getClientId() == clientId)
			{
				tokensMan.removeToken(token.getType(), token.getValue());
			}
		}
	}
}
