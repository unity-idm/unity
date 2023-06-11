/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Date;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;

class TokenUtils
{
	static Date getAccessTokenExpiration(OAuthASProperties config, Date now)
	{
		int accessTokenValidity = config.getAccessTokenValidity();

		return new Date(now.getTime() + accessTokenValidity * 1000);
	}

	static JWT decodeIDToken(OAuthToken internalToken)
	{
		try
		{
			return internalToken.getOpenidInfo() == null ? null : SignedJWT.parse(internalToken.getOpenidInfo());
		} catch (java.text.ParseException e)
		{
			throw new InternalException("Can not parse the internal id token", e);
		}
	}
}
