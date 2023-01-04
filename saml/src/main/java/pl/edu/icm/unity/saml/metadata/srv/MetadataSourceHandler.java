/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.metadata.cfg.AsyncExternalLogoFileDownloader;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Downloads metadata from a single remote source. Maintains a list of consumers and allows
 * for registering new and deregistering existing. Deregistration of the last consumer 
 * causes the object to stop its operation (it can be reclaimed or kept by the wrapping code). 
 * <p>
 * For simplicity this handler is triggered at a constant (quite high) rate, 
 * so that refresh interval changes do not require rescheduling of currently scheduled task.
 *  
 * @author K. Benedyczak
 */
class MetadataSourceHandler
{
	private static final Duration MAX_REFRESH_INTERVAL = Duration.ofDays(365);
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML,
			MetadataSourceHandler.class);
	private static final Duration DEFAULT_RERUN_INTERVAL = Duration.ofSeconds(5);
	private static final Duration INITIAL_REFRESH_DELAY = Duration.ofMillis(2);
	private final Duration rerunInterval;
	private final RemoteMetadataSrc source;
	private final ExecutorsService executorsService;
	private final CachedMetadataLoader downloader;
	private final AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader;

	private Duration refreshInterval;
	private Instant lastRefresh;
	private Map<String, MetadataConsumer> consumersById = new HashMap<>();
	private ScheduledFuture<?> scheduleWithFixedDelay;
	
	MetadataSourceHandler(RemoteMetadataSrc source, ExecutorsService executorsService,
			CachedMetadataLoader downloader, AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader)
	{
		this(source, executorsService, downloader, DEFAULT_RERUN_INTERVAL, asyncExternalLogoFileDownloader);
	}
	
	MetadataSourceHandler(RemoteMetadataSrc source, ExecutorsService executorsService,
			CachedMetadataLoader downloader, Duration reRunInterval, AsyncExternalLogoFileDownloader asyncExternalLogoFileDownloader)
	{
		this.source = source;
		this.executorsService = executorsService;
		this.downloader = downloader;
		this.rerunInterval = reRunInterval;
		this.asyncExternalLogoFileDownloader = asyncExternalLogoFileDownloader;
	}

	synchronized RemoteMetadataSrc getSource()
	{
		return source;
	}
	
	synchronized void addConsumer(MetadataConsumer consumer)
	{
		boolean logoDownloadDisabled = consumersById.values().stream().noneMatch(con -> con.logoDownload);
		consumersById.put(consumer.id, consumer);
		refreshInterval = getNewRefreshInterval();
		boolean addedFirstConsumer = consumersById.size() == 1;
		feedWithCached(consumer);
		if (addedFirstConsumer)
			startRefresh();
		else if(logoDownloadDisabled && consumer.logoDownload)
		{
			Optional<EntitiesDescriptorDocument> cached;
			try {
				cached = downloader.getCached(source.url);
			} catch (XmlException | IOException | InterruptedException e) {
				cached = Optional.empty();
			}
			cached.ifPresent(matadata -> asyncExternalLogoFileDownloader.downloadLogoFilesAsync(matadata, source.truststore));
		}
	}

	/**
	 * @return true if this was the last consumer
	 */
	synchronized boolean removeConsumer(String id)
	{
		consumersById.remove(id);
		refreshInterval = getNewRefreshInterval();
		if (consumersById.isEmpty())
			stopRefresh();
		return consumersById.isEmpty();
	}

	synchronized Duration getRefreshInterval()
	{
		return refreshInterval;
	}

	private void stopRefresh()
	{
		try
		{
			log.debug("Stopping refreshing of {} metadata", source.url);
			scheduleWithFixedDelay.cancel(false);
		} catch (Exception e)
		{
			log.warn("Error stopping metadata task", e);
		}
	}

	private void startRefresh()
	{
		scheduleWithFixedDelay = executorsService.getScheduledService().scheduleWithFixedDelay(
				this::refresh, 
				INITIAL_REFRESH_DELAY.toMillis(), rerunInterval.toMillis(), TimeUnit.MILLISECONDS);
		log.debug("Started refreshing of {} metadata", source.url);
	}
	
	
	private Duration getNewRefreshInterval()
	{
		Duration interval = MAX_REFRESH_INTERVAL;
		for (MetadataConsumer consumer: consumersById.values())
			if (consumer.refreshInterval.compareTo(interval) < 0)
				interval = consumer.refreshInterval;
		return interval;
	}
	
	private void refresh()
	{
		try
		{
			log.trace("Re-running refresh task");
			if (isRefreshNeeded())
				doRefresh();
		} catch (Exception e)
		{
			log.error("Error in metadata refresh task", e);
		}
		log.trace("Refresh task finished");
	}

	private synchronized boolean isRefreshNeeded()
	{
		long refreshIntervalMs = refreshInterval.toMillis();
		long sinceLastRefresh = lastRefresh == null ? 
				Long.MAX_VALUE : lastRefresh.until(Instant.now(), ChronoUnit.MILLIS);
		if (sinceLastRefresh >= refreshIntervalMs)
		{
			lastRefresh = Instant.now();
			return true;
		} else
		{
			log.trace("Metadata for {} is fresh, refresh needed in {}ms", source.url,
					refreshIntervalMs - sinceLastRefresh);
			return false;
		}
	}

	
	private void doRefresh()
	{
		log.info("Refreshing metadata for {}, (current refresh interval is {}s)", 
				source.url, refreshInterval.toSeconds());
		Stopwatch watch = Stopwatch.createStarted();
		EntitiesDescriptorDocument metadata;
		try
		{
			metadata = downloader.getFresh(source.url, source.truststore);
		} catch (Exception e)
		{
			log.error("Error downloading fresh metadata from " + source.url, e);
			return;
		}
		if(consumersById.values().stream().anyMatch(consumer -> consumer.logoDownload))
			asyncExternalLogoFileDownloader.downloadLogoFilesAsync(metadata, source.truststore);
		notifyConsumers(metadata);
		log.info("Metadata refresh for {} done in {}", source.url, watch);
	}

	private void feedWithCached(MetadataConsumer consumer)
	{
		Optional<EntitiesDescriptorDocument> metadata;
		try
		{
			metadata = downloader.getCached(source.url);
		} catch (Exception e)
		{
			log.error("Error loading cached metadata of " + source.url, e);
			return;
		}

		if (metadata.isPresent())
		{
			log.debug("Providing cached metadata for new consumer of {}", source.url);
			notifyConsumer(consumer, metadata.get());
			return;
		} else
		{
			log.debug("No cached metadata for new consumer of {}", source.url);
			return;
		}
	}
	
	private void notifyConsumers(EntitiesDescriptorDocument metadata)
	{
		Collection<MetadataConsumer> consumersCopy;
		synchronized(this)
		{
			consumersCopy = new ArrayList<>(consumersById.values());
		}
		consumersCopy.forEach(consumer -> notifyConsumer(consumer, metadata));
	}

	private void notifyConsumer(MetadataConsumer consumer, EntitiesDescriptorDocument metadata)
	{
		try
		{
			log.debug("Pushing metadata {} to consumer {}", source.url, consumer.id);
			consumer.consumer.accept(metadata, consumer.id);
		} catch (Exception e)
		{
			log.error("Metadata consumer failed to accept new metadata", e);
		}
	}
}
