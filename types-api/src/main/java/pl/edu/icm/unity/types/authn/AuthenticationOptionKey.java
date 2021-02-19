/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Objects;

/**
 * Represents an authentication option, which is a pair of authenticator id and one of its authentication option ids.
 */
public class AuthenticationOptionKey
{
	private String authenticatorKey;
	private String optionKey;

	
	public AuthenticationOptionKey(String authenticatorKey, String optionKey)
	{
		this.authenticatorKey = authenticatorKey;
		this.optionKey = optionKey;
	}
	
	public static AuthenticationOptionKey valueOf(String stringEncodedKey)
	{
		return new AuthenticationOptionKey(
				AuthenticationOptionKeyUtils.decodeAuthenticator(stringEncodedKey), 
				AuthenticationOptionKeyUtils.decodeOption(stringEncodedKey)
		);
	}
	
	public String toStringEncodedKey()
	{
		return AuthenticationOptionKeyUtils.encode(authenticatorKey, optionKey);
	}

	String getAuthenticatorKey()
	{
		return authenticatorKey;
	}

	String getOptionKey()
	{
		return optionKey;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof AuthenticationOptionKey))
			return false;
		AuthenticationOptionKey castOther = (AuthenticationOptionKey) other;
		return Objects.equals(authenticatorKey, castOther.authenticatorKey)
				&& Objects.equals(optionKey, castOther.optionKey);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(authenticatorKey, optionKey);
	}
}
