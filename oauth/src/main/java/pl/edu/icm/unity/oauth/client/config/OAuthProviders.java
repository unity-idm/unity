/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OAuthProviders
{
	private final Map<OAuthProviderKey, OAuthProviderConfiguration> providers;

	public OAuthProviders(Collection<OAuthProviderConfiguration> providers)
	{
		this.providers = providers.stream()
				.collect(Collectors.toUnmodifiableMap(p -> p.key, p -> p));
	}

	public OAuthProviderConfiguration get(OAuthProviderKey key)
	{
		OAuthProviderConfiguration provider = providers.get(key);
		if (provider == null)
			throw new IllegalArgumentException("There is no OAuth provider with key " + key);
		return provider;
	}

	public boolean contains(OAuthProviderKey key)
	{
		return providers.containsKey(key);
	}

	public Collection<OAuthProviderConfiguration> getAll()
	{
		return Collections.unmodifiableCollection(providers.values());
	}

	public Set<OAuthProviderKey> getKeys()
	{
		return Collections.unmodifiableSet(providers.keySet());
	}

	public Set<Map.Entry<OAuthProviderKey, OAuthProviderConfiguration>> getEntrySet()
	{
		return Collections.unmodifiableSet(providers.entrySet());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(providers);
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
		OAuthProviders other = (OAuthProviders) obj;
		return Objects.equals(providers, other.providers);
	}
}
