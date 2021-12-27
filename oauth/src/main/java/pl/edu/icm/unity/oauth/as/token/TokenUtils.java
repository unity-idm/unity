/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Utility methods for handling tokens
 * 
 * @author K. Benedyczak
 */
class TokenUtils
{
	private static Logger log = Log.getLogger(Log.U_SERVER_OAUTH, TokenUtils.class);

	
	static RefreshToken addRefreshToken(OAuthASProperties config, TokensManagement tokensManagement, 
			Date now, OAuthToken newToken, Long owner) throws EngineException, JsonProcessingException
	{
		RefreshToken refreshToken = getRefreshToken(config, newToken.isAcceptRefreshToken(),
				Arrays.asList(newToken.getEffectiveScope()).contains(OIDCScopeValue.OFFLINE_ACCESS.getValue()));
		if (refreshToken != null)
		{
			newToken.setRefreshToken(refreshToken.getValue());
			Date refreshExpiration = getRefreshTokenExpiration(config, now);
			log.info("Issuing new refresh token {}, valid until {}", BaseOAuthResource.tokenToLog(refreshToken.getValue()), 
					refreshExpiration);
			
			tokensManagement.addToken(OAuthProcessor.INTERNAL_REFRESH_TOKEN,
					refreshToken.getValue(),
					new EntityParam(owner),
					newToken.getSerialized(), now, refreshExpiration);
		}
		return refreshToken;
	}
	
	private static Date getRefreshTokenExpiration(OAuthASProperties config, Date now)
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
	
	private static RefreshToken getRefreshToken(OAuthASProperties config, boolean acceptRefreshToken, boolean offlineAccessRequested)
	{
		RefreshToken refreshToken = null;
		if (config.getRefreshTokenIssuePolicy().equals(RefreshTokenIssuePolicy.ALWAYS)
				|| (config.getRefreshTokenIssuePolicy().equals(RefreshTokenIssuePolicy.OFFLINE_SCOPE_BASED)
						&& acceptRefreshToken && offlineAccessRequested))
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
	
	static AccessTokenResponse getAccessTokenResponse(OAuthToken internalToken,
			AccessToken accessToken, RefreshToken refreshToken,
			Map<String, Object> additionalParams)
	{
		JWT signedJWT = decodeIDToken(internalToken);
		AccessTokenResponse oauthResponse = signedJWT == null
				? new AccessTokenResponse(new Tokens(accessToken, refreshToken),
						additionalParams)
				: new OIDCTokenResponse(new OIDCTokens(signedJWT, accessToken,
						refreshToken), additionalParams);
		return oauthResponse;
	}
	
	private static JWT decodeIDToken(OAuthToken internalToken)
	{
		try
		{
			return internalToken.getOpenidInfo() == null ? null : 
				SignedJWT.parse(internalToken.getOpenidInfo());
		} catch (ParseException e)
		{
			throw new InternalException("Can not parse the internal id token", e);
		}
	}
}
