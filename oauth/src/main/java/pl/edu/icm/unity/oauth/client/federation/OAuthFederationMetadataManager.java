/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class OAuthFederationMetadataManager
{
	private final ConcurrentHashMap<String, OAuthFederationEntityStatementConfig> configurations =
			new ConcurrentHashMap<>();

	public void updateConfiguration(String authenticatorName, OAuthFederationEntityStatementConfig config)
	{
		if (config == null)
			configurations.remove(authenticatorName);
		else
			configurations.put(authenticatorName, config);
	}

	public OAuthFederationEntityStatementConfig getConfiguration(String authenticatorName)
	{
		return configurations.get(authenticatorName);
	}
}
