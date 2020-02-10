/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.text.ParseException;
import java.util.Optional;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

public class BearerJWTAccessToken extends BearerAccessToken
{
	private final JWTClaimsSet claimsSet;
	
	public BearerJWTAccessToken(String value, int lifetime, Scope scope, JWTClaimsSet claimsSet)
	{
		super(value, lifetime, scope);
		this.claimsSet = claimsSet;
	}

	public JWTClaimsSet getClaimsSet()
	{
		return claimsSet;
	}
	

	public static Optional<SignedJWT> tryParseJWT(String tokenValue)
	{
		if (!tokenValue.contains(".")) //fail fast
			return Optional.empty();
		try
		{
			return Optional.of(SignedJWT.parse(tokenValue));
		} catch (ParseException e)
		{
			//we are assuming not an JWT...
			return Optional.empty();
		}
	}

	public static Optional<JWTClaimsSet> tryParseJWTClaimSet(String tokenValue)
	{
		return tryParseJWTClaimSet(tryParseJWT(tokenValue));
	}
	
	public static Optional<JWTClaimsSet> tryParseJWTClaimSet(Optional<SignedJWT> jwt)
	{
		if (!jwt.isPresent())
			return Optional.empty();
		try
		{
			return Optional.of(jwt.get().getJWTClaimsSet());
		} catch (ParseException e)
		{
			//we are assuming not an JWT...
			return Optional.empty();
		}
	}
}
