/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.springframework.stereotype.Component;

import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;

import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

@Component
class OAuthFederationServiceImpl implements OAuthFederationService
{
	private final ExecutorsService executorsService;
	private final Map<String, OAuthFederationSourceHandler> handlersByTrustAnchor = new ConcurrentHashMap<>();
	private final Map<String, String> consumerToTrustAnchor = new ConcurrentHashMap<>();

	OAuthFederationServiceImpl(ExecutorsService executorsService)
	{
		this.executorsService = executorsService;
	}

	@Override
	public String preregisterConsumer(String trustAnchorUrl)
	{
		String id = UUID.randomUUID().toString();
		consumerToTrustAnchor.put(id, trustAnchorUrl);
		return id;
	}

	@Override
	public void registerConsumer(String key, Duration refreshInterval, OAuthFederationConfig config,
			BiConsumer<List<TrustChain>, String> consumer)
	{
		String trustAnchorUrl = config.trustAnchorEntityId().getValue();
		OAuthFederationSourceHandler handler = handlersByTrustAnchor.computeIfAbsent(
				trustAnchorUrl,
				url -> new OAuthFederationSourceHandler(executorsService, new OAuthFederationLoader()));
		consumerToTrustAnchor.put(key, trustAnchorUrl);
		handler.addConsumer(key, refreshInterval, config, consumer);
	}

	@Override
	public void unregisterConsumer(String id)
	{
		if (id == null)
			return;
		String trustAnchorUrl = consumerToTrustAnchor.remove(id);
		if (trustAnchorUrl == null)
			return;
		OAuthFederationSourceHandler handler = handlersByTrustAnchor.get(trustAnchorUrl);
		if (handler == null)
			return;
		handler.removeConsumer(id);
	}
}
