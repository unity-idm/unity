/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.Properties;

import eu.unicore.util.configuration.PropertiesHelper;

/**
 * Implementations of this interface are used to instantiate configuration object of a particular storage engine.
 * The implementation will be available via {@link StorageConfiguration} bean. The name of the implementation bean
 * must be of the following form: "storageEngineConfig" + {@link StorageEngine} name.
 * @author K. Benedyczak
 */
public interface StorageConfigurationFactory
{
	public static final String BEAN_PFX = "storageEngineConfig";
	
	public PropertiesHelper getInstance(Properties src);
}
