/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.oauth.client.InstanceId;

class OAuthFederationMetadataManagerTest
{
	private static final String AUTH_NAME = "myAuthenticator";

	private OAuthFederationMetadataManager manager;

	@BeforeEach
	void setUp()
	{
		manager = new OAuthFederationMetadataManager();
	}

	@Test
	void shouldReturnNullForUnknownAuthenticator()
	{
		assertThat(manager.getConfiguration(AUTH_NAME)).isNull();
	}

	@Test
	void shouldStoreAndReturnConfiguration()
	{
		InstanceId instanceId = new InstanceId();
		OAuthFederationEntityStatementConfig config = makeConfig("entityA");

		manager.updateConfiguration(AUTH_NAME, config, instanceId);

		assertThat(manager.getConfiguration(AUTH_NAME)).isSameAs(config);
	}

	@Test
	void shouldRemoveConfigurationWhenInstanceIdMatches()
	{
		InstanceId instanceId = new InstanceId();
		manager.updateConfiguration(AUTH_NAME, makeConfig("entityA"), instanceId);

		manager.updateConfiguration(AUTH_NAME, null, instanceId);

		assertThat(manager.getConfiguration(AUTH_NAME)).isNull();
	}

	@Test
	void destroyOfOldInstanceShouldNotWipeConfigurationSetByNewInstance()
	{
		InstanceId oldInstanceId = new InstanceId();
		InstanceId newInstanceId = new InstanceId();
		OAuthFederationEntityStatementConfig newConfig = makeConfig("entityB");

		manager.updateConfiguration(AUTH_NAME, makeConfig("entityA"), oldInstanceId);
		manager.updateConfiguration(AUTH_NAME, newConfig, newInstanceId);
		manager.updateConfiguration(AUTH_NAME, null, oldInstanceId);

		assertThat(manager.getConfiguration(AUTH_NAME)).isSameAs(newConfig);
	}

	@Test
	void shouldAllowSameInstanceToReplaceItsOwnConfiguration()
	{
		InstanceId instanceId = new InstanceId();
		OAuthFederationEntityStatementConfig first = makeConfig("entityA");
		OAuthFederationEntityStatementConfig second = makeConfig("entityB");

		manager.updateConfiguration(AUTH_NAME, first, instanceId);
		manager.updateConfiguration(AUTH_NAME, second, instanceId);

		assertThat(manager.getConfiguration(AUTH_NAME)).isSameAs(second);
	}

	private static OAuthFederationEntityStatementConfig makeConfig(String entityId)
	{
		return new OAuthFederationEntityStatementConfig(entityId, null, null, "https://cb", null, 3600);
	}
}
