/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Objects;

/**
 * Represents selection of authentication options: either a single concrete one or all options 
 * under a common authenticator.
 */
public class AuthenticationOptionsSelector implements Comparable<AuthenticationOptionsSelector>
{
	private static final String ALL_OPTS = "*";
	
	private String authenticatorKey;
	private String optionKey;

	public AuthenticationOptionsSelector(String authenticatorKey, String optionKey)
	{
		this.authenticatorKey = authenticatorKey;
		this.optionKey = optionKey;
	}
	
	protected AuthenticationOptionsSelector()
	{
	}

	public static AuthenticationOptionsSelector allForAuthenticator(String authenticatorKey)
	{
		return new AuthenticationOptionsSelector(authenticatorKey, ALL_OPTS);
	}
	
	public static AuthenticationOptionsSelector valueOf(String stringEncodedSelector)
	{
		String option = stringEncodedSelector.contains(".") ? 
				AuthenticationOptionKeyUtils.decodeOption(stringEncodedSelector) : ALL_OPTS;
		return new AuthenticationOptionsSelector(
				AuthenticationOptionKeyUtils.decodeAuthenticator(stringEncodedSelector), 
				option
		);
	}
	
	public String toStringEncodedSelector()
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

	public boolean matchesAuthnOption(AuthenticationOptionKey authnOptionKey)
	{
		if (!authnOptionKey.getAuthenticatorKey().equals(authenticatorKey))
			return false;
		
		return optionKey.equals(AuthenticationOptionsSelector.ALL_OPTS) ? 
				true : authnOptionKey.getOptionKey().equals(optionKey);
			
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof AuthenticationOptionsSelector))
			return false;
		AuthenticationOptionsSelector castOther = (AuthenticationOptionsSelector) other;
		return Objects.equals(authenticatorKey, castOther.authenticatorKey)
				&& Objects.equals(optionKey, castOther.optionKey);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(authenticatorKey, optionKey);
	}

	@Override
	public int compareTo(AuthenticationOptionsSelector o2)
	{
		if (getOptionKey().equals(AuthenticationOptionsSelector.ALL_OPTS)
				&& o2.getOptionKey().equals(AuthenticationOptionsSelector.ALL_OPTS))
		{
			return getAuthenticatorKey().compareTo(o2.getAuthenticatorKey());
		} else if (getOptionKey().equals(AuthenticationOptionsSelector.ALL_OPTS)
				&& !o2.getOptionKey().equals(AuthenticationOptionsSelector.ALL_OPTS))
		{
			return -1;
		} else if (!getOptionKey().equals(AuthenticationOptionsSelector.ALL_OPTS)
				&& o2.getOptionKey().equals(AuthenticationOptionsSelector.ALL_OPTS))
		{
			return 1;
		} else
		{
			return toStringEncodedSelector().compareTo(o2.toStringEncodedSelector());
		}
	}
}
