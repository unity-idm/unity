/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

class OAuthFederationSourceHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthFederationSourceHandler.class);
	private static final Duration RERUN_INTERVAL = Duration.ofSeconds(5);

	private final OAuthFederationLoader loader;
	private final Map<String, ConsumerEntry> consumers = new LinkedHashMap<>();
	private final ScheduledExecutorService scheduler;
	private ScheduledFuture<?> scheduledTask;
	private Instant lastRefresh = Instant.EPOCH;

	OAuthFederationSourceHandler(ExecutorsService executorsService, OAuthFederationLoader loader)
	{
		this.loader = loader;
		this.scheduler = executorsService.getScheduledService();
	}

	synchronized void addConsumer(String id, Duration refreshInterval, OAuthFederationConfig config,
			BiConsumer<List<TrustChain>, String> consumer)
	{
		boolean wasEmpty = consumers.isEmpty();
		consumers.put(id, new ConsumerEntry(id, refreshInterval, config, consumer));
		if (scheduledTask == null)
			scheduledTask = scheduler.scheduleWithFixedDelay(this::refresh,
					RERUN_INTERVAL.toSeconds(), RERUN_INTERVAL.toSeconds(), TimeUnit.SECONDS);
		else if (wasEmpty)
			lastRefresh = Instant.EPOCH;
	}

	synchronized boolean removeConsumer(String id)
	{
		consumers.remove(id);
		return consumers.isEmpty();
	}

	synchronized void cancel()
	{
		if (scheduledTask != null)
		{
			scheduledTask.cancel(false);
			scheduledTask = null;
		}
	}

	private void refresh()
	{
		List<ConsumerEntry> toRefresh;
		synchronized (this)
		{
			boolean globalIntervalElapsed = Duration.between(lastRefresh, Instant.now())
					.compareTo(consumers.values().stream()
							.map(ConsumerEntry::refreshInterval)
							.min(Duration::compareTo)
							.orElse(Duration.ofSeconds(Long.MAX_VALUE))) >= 0;
			boolean hasExpired = loader.hasExpiredEntries();
			if (!globalIntervalElapsed && !hasExpired)
				return;
			toRefresh = new ArrayList<>(consumers.values());
		}
		for (ConsumerEntry entry : toRefresh)
		{
			try
			{
				List<TrustChain> chains = loader.loadAll(entry.config);
				entry.consumer.accept(chains, entry.id);
				synchronized (this)
				{
					lastRefresh = Instant.now();
				}
			} catch (Exception e)
			{
				log.error("Error refreshing federation providers for consumer {}, will retry in {}s",
						entry.id, RERUN_INTERVAL.toSeconds(), e);
			}
		}
	}

	static String generateConsumerId()
	{
		return UUID.randomUUID().toString();
	}

	private record ConsumerEntry(
			String id,
			Duration refreshInterval,
			OAuthFederationConfig config,
			BiConsumer<List<TrustChain>, String> consumer) {}
}
