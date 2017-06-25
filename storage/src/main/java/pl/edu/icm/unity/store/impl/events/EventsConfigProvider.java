/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.events;

import org.springframework.stereotype.Component;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapIndexConfig;

import pl.edu.icm.unity.store.hz.MapConfigProvider;


/**
 * Configures events map: index on nextProcessing
 * @author K. Benedyczak
 */
@Component
public class EventsConfigProvider implements MapConfigProvider
{
	@Override
	public MapConfig getMapConfig()
	{
		MapConfig mapConfig = new MapConfig();
		mapConfig.setName(EventHzStore.STORE_ID);

		MapIndexConfig eventIndexCfg = new MapIndexConfig("nextProcessing", false);
		mapConfig.addMapIndexConfig(eventIndexCfg);
		return mapConfig;
	}
}
