/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.Properties;

import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.PropertiesHelper;
import pl.edu.icm.unity.store.StorageConfigurationFactory;

/**
 * Provider of {@link HzConfiguration}
 * @author K. Benedyczak
 */
@Component(StorageConfigurationFactory.BEAN_PFX + "hz")
public class HzConfigurationFactory implements StorageConfigurationFactory 
{
	@Override
	public PropertiesHelper getInstance(Properties src)
	{
		return new HzConfiguration(src);
	}

}
