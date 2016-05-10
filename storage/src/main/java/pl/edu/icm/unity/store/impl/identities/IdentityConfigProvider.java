/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapIndexConfig;

import pl.edu.icm.unity.store.hz.MapConfigProvider;


/**
 * Configures identities map: index on entityId
 * @author K. Benedyczak
 * FIXME - must be reenabled after we have clean up with serialization and types. Currently
 * serialization depends on IdentityTypesDAO, what breaks indexing 
 */
//@Component
public class IdentityConfigProvider implements MapConfigProvider
{
	@Override
	public MapConfig getMapConfig()
	{
		MapConfig mapConfig = new MapConfig();
		mapConfig.setName(IdentityHzStore.STORE_ID);

		MapIndexConfig entityIndexCfg = new MapIndexConfig("entityId", false);
		mapConfig.addMapIndexConfig(entityIndexCfg);
		return mapConfig;
	}
}
