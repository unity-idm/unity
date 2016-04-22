/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.io.IOException;
import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.FilePropertiesHelper;

/**
 * Loads configuration properties. As a class so that we can better test configuration dependent services
 * by mocking the loader.
 * 
 * @author Krzysztof Benedyczak
 */
public class ConfigurationLoader
{
	/**
	 * Loads configuration from a given file path.
	 * @param location
	 * @return
	 */
	public Properties getProperties(String location)
	{
		try
		{
			return FilePropertiesHelper.load(location);
		} catch (IOException e)
		{
			throw new ConfigurationException("Can not load configuration from " 
					+ location, e);
		}
	}
}
