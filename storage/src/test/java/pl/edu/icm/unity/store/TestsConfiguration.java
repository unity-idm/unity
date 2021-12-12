/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.Properties;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.StoragePropertiesSource;

@Component
public class TestsConfiguration implements StoragePropertiesSource
{
	@Override
	public Properties getProperties()
	{
		Properties ret = new Properties();
		fillH2Properties(ret);
		return ret;
	}
	
	private void fillH2Properties(Properties ret)
	{
		ret.setProperty("unityServer.storage.engine", "rdbms");
		ret.setProperty("unityServer.storage.engine.rdbms.jdbcUrl", 
				"jdbc:h2:file:./target/data/unitydb.bin");
	}
}
