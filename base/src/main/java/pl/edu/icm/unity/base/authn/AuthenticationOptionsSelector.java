/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.authn;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.JsonUtil;

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
	
	public AuthenticationOptionsSelector(String authenticatorKey, String optionKey)
	{
		this(authenticatorKey, optionKey, Optional.empty());
	}
	
	@JsonCreator
	public AuthenticationOptionsSelector(JsonNode json)
	{
		if (json.isTextual())
		{
			String[] specs = json.asText().split("\\.");
			if (specs.length != 2)
				throw new IllegalArgumentException("Invalid selector format: " + json.toString());
			this.authenticatorKey = specs[0];
			this.optionKey = specs[1];
		} else
		{
			if (!JsonUtil.notNull(json, "authenticatorKey"))
				throw new IllegalArgumentException("Expecting authenticatorKey in json object: " + json.toString());
			if (!JsonUtil.notNull(json, "optionKey"))
				throw new IllegalArgumentException("Expecting optionKey in json object: " + json.toString());
			
			this.authenticatorKey = JsonUtil.getWithDef(json, "authenticatorKey", null);
			this.optionKey = JsonUtil.getWithDef(json, "optionKey", null);
		}
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
