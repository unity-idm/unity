/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.text.ParseException;
import java.util.Optional;

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

	/**
	 * @return empty if the JWKS is valid, or the underlying parser's error detail otherwise
	 */
	public static Optional<String> validationError(String jwks)
	{
		try
		{
			JWKSet.parse(jwks);
			return Optional.empty();
		} catch (ParseException e)
		{
			return Optional.of(e.getMessage());
		}
	}
}
