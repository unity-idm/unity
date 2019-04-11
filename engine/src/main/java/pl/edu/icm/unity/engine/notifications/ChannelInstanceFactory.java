/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;
import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Produces and caches instances of {@link NotificationChannelInstance}.
 * 
 * @author K. Benedyczak
 */
@Component
class ChannelInstanceFactory
{
	private static final String CACHE_ID = NotificationProducerImpl.class.getName() + "_cache";
	private Ehcache channelsCache;
	private NotificationFacilitiesRegistry facilitiesRegistry;
	private NotificationChannelDB channelDB;

	@Autowired
	ChannelInstanceFactory(CacheProvider cacheProvider, 
			NotificationFacilitiesRegistry facilitiesRegistry,
			NotificationChannelDB channelDB)
	{
		this.facilitiesRegistry = facilitiesRegistry;
		this.channelDB = channelDB;
		initCache(cacheProvider.getManager());
	}

	NotificationChannelInstance loadChannel(String channelName)
	{
		Element cachedChannel = channelsCache.get(channelName);
		NotificationChannelInstance channel;
		if (cachedChannel == null)
		{
			channel = loadFromDb(channelName);
		} else
			channel = (NotificationChannelInstance) cachedChannel.getObjectValue();
		
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
	
	private void initCache(CacheManager cacheManager)
	{
		channelsCache = cacheManager.addCacheIfAbsent(CACHE_ID);
		CacheConfiguration config = channelsCache.getCacheConfiguration();
		config.setTimeToIdleSeconds(120);
		config.setTimeToLiveSeconds(120);
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		config.persistence(persistCfg);
	}
}
