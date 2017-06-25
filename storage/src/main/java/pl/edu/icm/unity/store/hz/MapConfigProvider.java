/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import com.hazelcast.config.MapConfig;

/**
 * Implementation beans provide configuration of maps with indices and other settings.
 * @author K. Benedyczak
 */
public interface MapConfigProvider
{
	MapConfig getMapConfig();
}
