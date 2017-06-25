/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.util.Properties;

import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.PropertiesHelper;
import pl.edu.icm.unity.store.StorageConfigurationFactory;

/**
 * Provider of {@link RDBMSConfiguration}.
 * @author K. Benedyczak
 */
@Component(StorageConfigurationFactory.BEAN_PFX + "rdbms")
public class RDBMSConfigurationFactory implements StorageConfigurationFactory 
{
	@Override
	public PropertiesHelper getInstance(Properties src)
	{
		return new RDBMSConfiguration(src);
	}

}
