/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.authn;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Represents an authentication option, which is a pair of authenticator id and one of its authentication option ids.
 */
public class AuthenticationOptionKey
{
	private final String authenticatorKey;
	private final String optionKey;

	
	public AuthenticationOptionKey(String authenticatorKey, String optionKey)
	{
		this.authenticatorKey = authenticatorKey;
		this.optionKey = optionKey;
		
		if (authenticatorKey == null)
			throw new IllegalArgumentException("authenticatorKey can not be null");
	}

	
	@JsonCreator
	private AuthenticationOptionKey(TextNode value)
	{
		this(AuthenticationOptionKeyUtils.decodeAuthenticator(value.asText()), 
				AuthenticationOptionKeyUtils.decodeOption(value.asText()));
	}
	
	public static AuthenticationOptionKey valueOf(String stringEncodedKey)
	{
		return new AuthenticationOptionKey(
				AuthenticationOptionKeyUtils.decodeAuthenticator(stringEncodedKey), 
				AuthenticationOptionKeyUtils.decodeOption(stringEncodedKey)
		);
	}

	public static AuthenticationOptionKey authenticatorOnlyKey(String authenticatorKey)
	{
		return new AuthenticationOptionKey(authenticatorKey, null);
	}

	
	@JsonValue
	public String toStringEncodedKey()
	{
		return AuthenticationOptionKeyUtils.encode(authenticatorKey, optionKey);
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


	@Override
	public String toString()
	{
		return toStringEncodedKey();
	}
	
	
}
