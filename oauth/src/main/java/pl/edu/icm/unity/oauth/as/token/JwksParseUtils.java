/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.text.ParseException;

import com.nimbusds.jose.jwk.JWKSet;

import pl.edu.icm.unity.base.exceptions.InternalException;

public class JwksParseUtils
{
	public static JWKSet parseRequired(String jwks, String contextDescription)
	{
		try
		{
			return JWKSet.parse(jwks);
		} catch (ParseException e)
		{
			throw new InternalException(contextDescription + ": " + e.getMessage());
		}
	}

	public static boolean isValidJwks(String jwks)
	{
		if (jwks == null || jwks.isBlank())
			return false;
		try
		{
			JWKSet.parse(jwks);
			return true;
		} catch (ParseException e)
		{
			return false;
		}
	}
}
