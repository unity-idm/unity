/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.client.InstanceId;

@Component
public class OAuthFederationMetadataManager
{
	private record ConfigEntry(OAuthFederationEntityStatementConfig config, InstanceId instanceId) {}

	private final ConcurrentHashMap<String, ConfigEntry> configurations = new ConcurrentHashMap<>();

	public void updateConfiguration(String authenticatorName, OAuthFederationEntityStatementConfig config,
			InstanceId instanceId)
	{
		if (config == null)
			configurations.compute(authenticatorName,
					(name, existing) -> existing != null && existing.instanceId == instanceId ? null : existing);
		else
			configurations.put(authenticatorName, new ConfigEntry(config, instanceId));
	}

	public OAuthFederationEntityStatementConfig getConfiguration(String authenticatorName)
	{
		ConfigEntry entry = configurations.get(authenticatorName);
		return entry == null ? null : entry.config();
	}
}
