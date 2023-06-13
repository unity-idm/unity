/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;

/**
 * Produces and caches instances of {@link NotificationChannelInstance}.
 */
@Component
class ChannelInstanceFactory
{
	private final Cache<String, NotificationChannelInstance> channelsCache;
	private final NotificationFacilitiesRegistry facilitiesRegistry;
	private final NotificationChannelDB channelDB;

	@Autowired
	ChannelInstanceFactory(
			NotificationFacilitiesRegistry facilitiesRegistry,
			NotificationChannelDB channelDB)
	{
		this.facilitiesRegistry = facilitiesRegistry;
		this.channelDB = channelDB;
		this.channelsCache = CacheBuilder.newBuilder()
				.expireAfterWrite(Duration.ofSeconds(120))
				.build();
	}

	NotificationChannelInstance loadChannel(String channelName)
	{
		NotificationChannelInstance cachedChannel = channelsCache.getIfPresent(channelName);
		NotificationChannelInstance channel = cachedChannel == null ? 
				loadFromDb(channelName) : cachedChannel;
		if (channel == null)
			throw new IllegalArgumentException("Channel " + channelName + " is not known");
		return channel;
	}
	
	private NotificationChannelInstance loadFromDb(String channelName)
	{
		NotificationChannel channelDesc = channelDB.get(channelName);
		NotificationFacility facility = facilitiesRegistry.getByName(channelDesc.getFacilityId());
		return facility.getChannel(channelDesc.getConfiguration());
	}
}
