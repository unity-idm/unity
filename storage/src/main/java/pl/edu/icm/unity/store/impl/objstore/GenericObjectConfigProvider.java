/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import org.springframework.stereotype.Component;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapIndexConfig;

import pl.edu.icm.unity.store.hz.MapConfigProvider;


/**
 * Configures tokens map: indices on owner, type and name.
 * @author K. Benedyczak
 */
@Component
public class GenericObjectConfigProvider implements MapConfigProvider
{
	@Override
	public MapConfig getMapConfig()
	{
		MapConfig mapConfig = new MapConfig();
		mapConfig.setName(GenericObjectHzStore.STORE_ID);

		MapIndexConfig typeIndexCfg = new MapIndexConfig("type", false);
		mapConfig.addMapIndexConfig(typeIndexCfg);
		MapIndexConfig valueIndexCfg = new MapIndexConfig("name", false);
		mapConfig.addMapIndexConfig(valueIndexCfg);
		return mapConfig;
	}
}
