/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.store.api.StoragePropertiesSource;

/**
 * Wires up the main configuration file properties as the source of the DB layer configuration
 * @author K. Benedyczak
 */
@Component
public class StorageConfigurationProviderImpl implements StoragePropertiesSource
{
	@Autowired
	private UnityServerConfiguration mainCfg;
	
	@Override
	public Properties getProperties()
	{
		return mainCfg.getProperties();
	}
}
