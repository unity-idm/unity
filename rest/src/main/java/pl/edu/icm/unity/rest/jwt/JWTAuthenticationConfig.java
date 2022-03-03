/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt;

public class JWTAuthenticationConfig
{
	public final long tokenTTLSeconds;
	public final String signingCredential;

	public JWTAuthenticationConfig(long tokenTTL, String signingCredential)
	{
		this.tokenTTLSeconds = tokenTTL;
		this.signingCredential = signingCredential;
	}
}
