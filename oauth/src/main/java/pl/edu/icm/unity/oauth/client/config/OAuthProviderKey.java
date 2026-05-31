/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Objects;

public class OAuthProviderKey
{
	private final String key;

	private OAuthProviderKey(String key)
	{
		this.key = key;
	}

	public static OAuthProviderKey fromConfig(String configurationEntryPrefix)
	{
		if (!configurationEntryPrefix.startsWith(OAuthClientProperties.PROVIDERS)
				|| !configurationEntryPrefix.endsWith("."))
			throw new IllegalArgumentException(configurationEntryPrefix
					+ " doesn't look like oauth provider config prefix");
		return new OAuthProviderKey(configurationEntryPrefix.substring(
				OAuthClientProperties.PROVIDERS.length(),
				configurationEntryPrefix.length() - 1));
	}

	public static OAuthProviderKey of(String key)
	{
		return new OAuthProviderKey(key);
	}
	
	public String asString()
	{
		return key;
	}

	@Override
	public String toString()
	{
		return String.format("OAuthProviderKey [key=%s]", key);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OAuthProviderKey other = (OAuthProviderKey) obj;
		return Objects.equals(key, other.key);
	}
}
