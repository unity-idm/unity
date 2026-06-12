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

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

@Component
class OAuthFederationServiceImpl implements OAuthFederationService
{
	private final ExecutorsService executorsService;
	private final Map<HandlerKey, OAuthFederationSourceHandler> handlersByKey = new ConcurrentHashMap<>();
	private final Map<String, HandlerKey> consumerToHandlerKey = new ConcurrentHashMap<>();

	OAuthFederationServiceImpl(ExecutorsService executorsService)
	{
		this.executorsService = executorsService;
	}

	@Override
	public String preregisterConsumer()
	{
		return UUID.randomUUID().toString();
	}

	@Override
	public void registerConsumer(String key, Duration refreshInterval, OAuthFederationConfig config,
			BiConsumer<List<TrustChain>, String> consumer)
	{
		HandlerKey handlerKey = HandlerKey.from(config);
		OAuthFederationSourceHandler handler = handlersByKey.computeIfAbsent(
				handlerKey,
				k -> new OAuthFederationSourceHandler(executorsService, new OAuthFederationLoader()));
		consumerToHandlerKey.put(key, handlerKey);
		handler.addConsumer(key, refreshInterval, config, consumer);
	}

	@Override
	public void unregisterConsumer(String id)
	{
		if (id == null)
			return;
		HandlerKey handlerKey = consumerToHandlerKey.remove(id);
		if (handlerKey == null)
			return;
		handlersByKey.compute(handlerKey, (k, handler) ->
		{
			if (handler == null)
				return null;
			boolean empty = handler.removeConsumer(id);
			if (empty)
			{
				handler.cancel();
				return null;
			}
			return handler;
		});
	}

	private record HandlerKey(String trustAnchorUrl, String truststore, ServerHostnameCheckingMode hostnameCheckingMode)
	{
		static HandlerKey from(OAuthFederationConfig config)
		{
			return new HandlerKey(config.trustAnchorEntityId().getValue(),
					config.truststore(), config.hostnameCheckingMode());
		}
	}
}
