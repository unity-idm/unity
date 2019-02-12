/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Objects;

/**
 * Represents a pair of authentication option with key.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class AuthenticationOptionKey
{
	public static final String ALL_OPTS = "*";
	
	private String authenticatorKey;
	private String optionKey;

	public AuthenticationOptionKey(String authenticatorKey, String optionKey)
	{
		this.authenticatorKey = authenticatorKey;
		this.optionKey = optionKey;
	}
	
	protected AuthenticationOptionKey()
	{
	}

	public static AuthenticationOptionKey valueOf(String globalKey)
	{
		return new AuthenticationOptionKey(
				AuthenticationOptionKeyUtils.decodeAuthenticator(globalKey), 
				AuthenticationOptionKeyUtils.decodeOption(globalKey)
		);
	}
	
	public String toGlobalKey()
	{
		return !optionKey.equals(ALL_OPTS) ? AuthenticationOptionKeyUtils.encode(authenticatorKey, optionKey)
				: AuthenticationOptionKeyUtils.encode(authenticatorKey, null);
	}

	public String getAuthenticatorKey()
	{
		return authenticatorKey;
	}

	public String getOptionKey()
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
