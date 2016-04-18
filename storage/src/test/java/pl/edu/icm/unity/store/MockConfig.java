/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.Properties;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.rdbms.StoragePropertiesSource;

@Component
public class MockConfig implements StoragePropertiesSource
{
	@Override
	public Properties getProperties()
	{
		Properties ret = new Properties();
		ret.setProperty("unityServer.db.jdbcUrl", "jdbc:h2:file:./target/data/unitydb.bin");
		return ret;
	}
}
