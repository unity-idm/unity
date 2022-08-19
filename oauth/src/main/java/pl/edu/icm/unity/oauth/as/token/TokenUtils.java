/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;

/**
 * Utility methods for handling tokens
 * 
 * @author K. Benedyczak
 */
class TokenUtils
{
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
