/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

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
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML,
			MetadataSourceHandler.class);
	private static final long DEFAULT_RERUN_INTERVAL = 5000;
	private final long rerunInterval;
	private final RemoteMetadataSrc source;
	private final ExecutorsService executorsService;
	private final MetadataDownloader downloader;

	private long refreshInterval;
	private Instant lastRefresh;
	private Map<String, MetadataConsumer> consumersById = new HashMap<>();
	private ScheduledFuture<?> scheduleWithFixedDelay;
	
	MetadataSourceHandler(RemoteMetadataSrc source, ExecutorsService executorsService,
			MetadataDownloader downloader)
	{
		this(source, executorsService, downloader, DEFAULT_RERUN_INTERVAL);
	}
	
	MetadataSourceHandler(RemoteMetadataSrc source, ExecutorsService executorsService,
			MetadataDownloader downloader, long reRunInterval)
	{
		this.source = source;
		this.executorsService = executorsService;
		this.downloader = downloader;
		this.rerunInterval = reRunInterval;
	}

	synchronized RemoteMetadataSrc getSource()
	{
		return source;
	}
	
	synchronized void addConsumer(MetadataConsumer consumer)
	{
		consumersById.put(consumer.id, consumer);
		refreshInterval = getNewRefreshInterval();
		if (!feedWithCached(consumer))
			scheduleQuickRefresh();
		if (consumersById.size() == 1)
			startRefresh();
	}
	
	/**
	 * Schedule an extra refresh
	 */
	private void scheduleQuickRefresh()
	{
		executorsService.getService().submit(this::doRefresh); 
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

	synchronized long getRefreshInterval()
	{
		return refreshInterval;
	}

	private void stopRefresh()
	{
		try
		{
			scheduleWithFixedDelay.cancel(true);
		} catch (Exception e)
		{
			log.warn("Error stopping metadata task", e);
		}
	}

	private void startRefresh()
	{
		scheduleWithFixedDelay = executorsService.getService().scheduleWithFixedDelay(
				this::refresh, 
				0, rerunInterval, TimeUnit.MILLISECONDS);
	}
	
	
	private long getNewRefreshInterval()
	{
		long interval = Long.MAX_VALUE;
		for (MetadataConsumer consumer: consumersById.values())
			if (consumer.refreshInterval < interval)
				interval = consumer.refreshInterval;
		return interval;
	}
	
	private void refresh()
	{
		if (isRefreshNeeded())
			doRefresh();
	}

	private synchronized boolean isRefreshNeeded()
	{
		long sinceLastRefresh = lastRefresh == null ? 
				Long.MAX_VALUE : lastRefresh.until(Instant.now(), ChronoUnit.MILLIS);
		if (sinceLastRefresh >= refreshInterval)
		{
			lastRefresh = Instant.now();
			return true;
		} else
		{
			log.trace("Metadata for {} is fresh, refresh needed in {}ms", source.url,
					refreshInterval - sinceLastRefresh);
			return false;
		}
	}

	
	private void doRefresh()
	{
		log.debug("Refreshing metadata for {}", source.url);
		EntitiesDescriptorDocument metadata;
		try
		{
			metadata = downloader.getFresh(source.url, source.truststore);
		} catch (Exception e)
		{
			log.error("Error downloading fresh metadata from " + source.url, e);
			return;
		}
		notifyConsumers(metadata);
	}

	private boolean feedWithCached(MetadataConsumer consumer)
	{
		Optional<EntitiesDescriptorDocument> metadata;
		try
		{
			metadata = downloader.getCached(source.url);
		} catch (Exception e)
		{
			log.error("Error loading cached metadata of " + source.url, e);
			return false;
		}
		if (metadata.isPresent())
		{
			log.debug("Providing cached metadata for new consumer of {}", source.url);
			notifyConsumer(consumer, metadata.get());
			return true;
		} else
		{
			log.debug("No cached metadata for new consumer of {}", source.url);
			return false;
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
