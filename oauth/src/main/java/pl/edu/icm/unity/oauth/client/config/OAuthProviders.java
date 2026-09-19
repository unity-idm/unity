/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record OAuthProviders(Map<OAuthProviderKey, OAuthProviderConfiguration> providers)
{
	public OAuthProviders(Collection<OAuthProviderConfiguration> providers)
	{
		this(providers.stream().collect(Collectors.toUnmodifiableMap(OAuthProviderConfiguration::key, p -> p)));
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

	public OAuthProviders replaceFederation(List<OAuthProviderConfiguration> federationProviders)
	{
		List<OAuthProviderConfiguration> merged = new ArrayList<>();
		providers.values().stream()
				.filter(p -> !p.key().isFromFederation())
				.forEach(merged::add);
		merged.addAll(federationProviders);
		return new OAuthProviders(merged);
	}

	public OAuthProviders overrideWithStatic(OAuthProviders staticProviders)
	{
		List<OAuthProviderConfiguration> merged = new ArrayList<>(providers.values());
		for (OAuthProviderConfiguration staticProvider : staticProviders.getAll())
		{
			merged.removeIf(p -> p.key().equals(staticProvider.key()));
			merged.add(staticProvider);
		}
		return new OAuthProviders(merged);
	}

	public Set<Map.Entry<OAuthProviderKey, OAuthProviderConfiguration>> getEntrySet()
	{
		return Collections.unmodifiableSet(providers.entrySet());
	}
}
