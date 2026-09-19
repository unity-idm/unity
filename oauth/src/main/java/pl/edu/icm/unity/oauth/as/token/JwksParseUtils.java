/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.text.ParseException;
import java.util.Optional;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import pl.edu.icm.unity.base.exceptions.InternalException;

public class JwksParseUtils
{
	public static JWKSet parseRequired(String jwks, String contextDescription)
	{
		JWKSet jwkSet = parseSet(jwks, contextDescription);
		extractionError(jwkSet).ifPresent(error ->
		{
			throw new InternalException(contextDescription + ": " + error);
		});
		return jwkSet;
	}

	/**
	 * @return empty if the JWKS is valid and a public key can be extracted from every entry,
	 *         or an error detail otherwise
	 */
	public static Optional<String> validationError(String jwks)
	{
		JWKSet jwkSet;
		try
		{
			jwkSet = JWKSet.parse(jwks);
		} catch (ParseException e)
		{
			return Optional.of(e.getMessage());
		}
		return extractionError(jwkSet);
	}

	private static JWKSet parseSet(String jwks, String contextDescription)
	{
		try
		{
			return JWKSet.parse(jwks);
		} catch (ParseException e)
		{
			throw new InternalException(contextDescription + ": " + e.getMessage());
		}
	}

	private static Optional<String> extractionError(JWKSet jwkSet)
	{
		if (jwkSet.getKeys().isEmpty())
			return Optional.of("JWK Set does not contain any keys");

		for (JWK jwk : jwkSet.getKeys())
		{
			try
			{
				if (toPublicKey(jwk) == null)
					return Optional.of("Key " + keyLabel(jwk) + " has an unsupported key type "
							+ jwk.getKeyType() + ", only RSA and EC keys are supported");
			} catch (JOSEException e)
			{
				return Optional.of("Cannot extract a public key from " + keyLabel(jwk) + ": "
						+ e.getMessage());
			}
		}
		return Optional.empty();
	}

	private static Object toPublicKey(JWK jwk) throws JOSEException
	{
		if (jwk instanceof RSAKey rsaKey)
			return rsaKey.toRSAPublicKey();
		if (jwk instanceof ECKey ecKey)
			return ecKey.toECPublicKey();
		return null;
	}

	private static String keyLabel(JWK jwk)
	{
		return jwk.getKeyID() != null ? "'" + jwk.getKeyID() + "'" : "key without kid";
	}
}
