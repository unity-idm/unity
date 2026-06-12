/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;

interface OAuthFederationService
{
	String preregisterConsumer();

	void registerConsumer(String key, Duration refreshInterval, OAuthFederationConfig config,
			BiConsumer<List<TrustChain>, String> consumer);

	void unregisterConsumer(String id);
}
