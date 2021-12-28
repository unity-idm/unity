/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;

/**
 * Represents selection of authentication options: either a single concrete one or all options 
 * under a common authenticator.
 */
public class AuthenticationOptionsSelector implements Comparable<AuthenticationOptionsSelector>
{
	public static final String ALL_OPTS = "*";
	
	public final String authenticatorKey;
	public final String optionKey;
	@JsonIgnore
	public final Optional<I18nString> displayedName;

	public AuthenticationOptionsSelector(String authenticatorKey, String optionKey, Optional<I18nString> displayedName)
	{
		this.authenticatorKey = authenticatorKey;
		this.optionKey = optionKey;
		this.displayedName = displayedName;
	}
	
	@JsonCreator
	public AuthenticationOptionsSelector(@JsonProperty("authenticatorKey") String authenticatorKey,
			@JsonProperty("optionKey") String optionKey)
	{
		this.authenticatorKey = authenticatorKey;
		this.optionKey = optionKey;
		this.displayedName = Optional.empty();
	}

	public static AuthenticationOptionsSelector allForAuthenticator(String authenticatorKey)
	{
		return new AuthenticationOptionsSelector(authenticatorKey, ALL_OPTS, Optional.empty());
	}
	
	public static AuthenticationOptionsSelector valueOf(String stringEncodedSelector)
	{
		String option = stringEncodedSelector.contains(".") ? 
				AuthenticationOptionKeyUtils.decodeOption(stringEncodedSelector) : ALL_OPTS;		
		return new AuthenticationOptionsSelector(
				AuthenticationOptionKeyUtils.decodeAuthenticator(stringEncodedSelector), 
				option);
	}
	
	public String toStringEncodedSelector()
	{
		return !optionKey.equals(ALL_OPTS) ? AuthenticationOptionKeyUtils.encode(authenticatorKey, optionKey)
				: AuthenticationOptionKeyUtils.encode(authenticatorKey, null);
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
		if (optionKey.equals(AuthenticationOptionsSelector.ALL_OPTS)
				&& o2.optionKey.equals(AuthenticationOptionsSelector.ALL_OPTS))
		{
			return authenticatorKey.compareTo(o2.authenticatorKey);
		} else if (optionKey.equals(AuthenticationOptionsSelector.ALL_OPTS)
				&& !o2.optionKey.equals(AuthenticationOptionsSelector.ALL_OPTS))
		{
			return -1;
		} else if (!optionKey.equals(AuthenticationOptionsSelector.ALL_OPTS)
				&& o2.optionKey.equals(AuthenticationOptionsSelector.ALL_OPTS))
		{
			return 1;
		} else
		{
			return toStringEncodedSelector().compareTo(o2.toStringEncodedSelector());
		}
	}

	public String getRepresentationFallbackToConfigKey(MessageSource msg)
	{
		return !displayedName.isEmpty() ? (authenticatorKey + ": " +  displayedName.get().getValue(msg)) : toStringEncodedSelector();
	}
	
	public static class AuthenticationOptionsSelectorComparator implements Comparator<AuthenticationOptionsSelector>
	{
		private MessageSource msg;
		
		public AuthenticationOptionsSelectorComparator(MessageSource msg)
		{
			this.msg = msg;
		}

		@Override
		public int compare(AuthenticationOptionsSelector arg0, AuthenticationOptionsSelector arg1)
		{
			if (arg0.authenticatorKey.equals(arg1.authenticatorKey))
				return arg0.getRepresentationFallbackToConfigKey(msg).compareTo(arg1.getRepresentationFallbackToConfigKey(msg));
			else
				return arg0.authenticatorKey.compareTo(arg1.authenticatorKey);			
		}	
	}
}
