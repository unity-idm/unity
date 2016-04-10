/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.util.Properties;

/**
 * Provides access to raw configuration of the storage layer, used to instantiate {@link DBConfiguration}.
 * @author K. Benedyczak
 */
public interface StoragePropertiesSource
{
	Properties getProperties();
}
