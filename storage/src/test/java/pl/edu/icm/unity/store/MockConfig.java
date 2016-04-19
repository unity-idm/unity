/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.rdbms.StoragePropertiesSource;

@Component
public class MockConfig implements StoragePropertiesSource
{
	@Autowired
	private Environment env;
	
	@Override
	public Properties getProperties()
	{
		Properties ret = new Properties();

		if (env.acceptsProfiles("test-storage-h2"))
			fillH2Properties(ret);
		else 
			fillHzH2Properties(ret);
			
		return ret;
	}
	
	private void fillHzH2Properties(Properties ret)
	{
		ret.setProperty("unityServer.storage.engine", "hz");
		ret.setProperty("unityServer.storage.engine.hz.jdbcUrl", "jdbc:h2:file:./target/data/unitydbWithHz.bin");
	}

	private void fillH2Properties(Properties ret)
	{
		ret.setProperty("unityServer.storage.engine", "rdbms");
		ret.setProperty("unityServer.storage.engine.rdbms.jdbcUrl", "jdbc:h2:file:./target/data/unitydb.bin");
	}
}
