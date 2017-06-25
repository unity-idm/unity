/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.Properties;

/**
 * Provides access to raw configuration of the storage layer, used to instantiate 
 * the actual configuration object of the storage layer.
 * 
 * This interface should be implemented by a bean. There is no default configuration in the 
 * storage module, besides test implementations.
 * 
 * @author K. Benedyczak
 */
public interface StoragePropertiesSource
{
	Properties getProperties();
}
