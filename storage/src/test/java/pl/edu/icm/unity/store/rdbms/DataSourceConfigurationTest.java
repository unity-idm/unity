/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import static org.junit.Assume.assumeTrue;
import static pl.edu.icm.unity.store.StorageConfiguration.ALTERNATIVE_DB_CONFIG;

import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageConfigurationFactory;
import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.store.api.StoragePropertiesSource;

public class DataSourceConfigurationTest
{
	@Before
	public void conditionalStart()
	{
		assumeTrue(System.getProperty(ALTERNATIVE_DB_CONFIG) == null);
	}
	
	@Test
	public void shouldSetDefaultNetworkTimeout() throws InternalException, IOException
	{
		// when
		PooledDataSource ds = getDataSourceWithConfig("unityServer.storage.engine.rdbms.defaultNetworkTimeout",
				"1234567891");

		// then
		Assertions.assertThat(ds.getDefaultNetworkTimeout()).isEqualTo(1234567891);
	}

	@Test
	public void shouldSetPoolMaximumActiveConnections() throws InternalException, IOException
	{
		// when
		PooledDataSource ds = getDataSourceWithConfig("unityServer.storage.engine.rdbms.poolMaximumActiveConnections",
				"1234567892");

		// then
		Assertions.assertThat(ds.getPoolMaximumActiveConnections()).isEqualTo(1234567892);
	}

	@Test
	public void shouldSetPoolMaximumIdleConnections() throws InternalException, IOException
	{
		// when
		PooledDataSource ds = getDataSourceWithConfig("unityServer.storage.engine.rdbms.poolMaximumIdleConnections",
				"1234567893");

		// then
		Assertions.assertThat(ds.getPoolMaximumIdleConnections()).isEqualTo(1234567893);
	}

	@Test
	public void shouldSetPoolMaximumCheckoutTime() throws InternalException, IOException
	{
		// when
		PooledDataSource ds = getDataSourceWithConfig("unityServer.storage.engine.rdbms.poolMaximumCheckoutTime",
				"1234567894");

		// then
		Assertions.assertThat(ds.getPoolMaximumCheckoutTime()).isEqualTo(1234567894);
	}

	@Test
	public void shouldSetPoolTimeToWait() throws InternalException, IOException
	{
		// when
		PooledDataSource ds = getDataSourceWithConfig("unityServer.storage.engine.rdbms.poolTimeToWait", "1234567895");

		// then
		Assertions.assertThat(ds.getPoolTimeToWait()).isEqualTo(1234567895);
	}

	@Test
	public void shouldSetPoolMaximumLocalBadConnectionTolerance() throws InternalException, IOException
	{
		// when
		PooledDataSource ds = getDataSourceWithConfig(
				"unityServer.storage.engine.rdbms.poolMaximumLocalBadConnectionTolerance", "1234567896");

		// then
		Assertions.assertThat(ds.getPoolMaximumLocalBadConnectionTolerance()).isEqualTo(1234567896);
	}

	@Test
	public void shouldSetPoolPingConnectionsNotUsedFor() throws InternalException, IOException
	{
		// when
		PooledDataSource ds = getDataSourceWithConfig("unityServer.storage.engine.rdbms.poolPingConnectionsNotUsedFor",
				"1234567897");

		// then
		Assertions.assertThat(ds.getPoolPingConnectionsNotUsedFor()).isEqualTo(1234567897);
	}

	private PooledDataSource getDataSourceWithConfig(String key, String value) throws InternalException, IOException
	{
		StorageConfigurationFactory factory = new RDBMSConfigurationFactory();
		StorageConfiguration storageConfig = new StorageConfiguration(
				ImmutableMap.of(StorageConfigurationFactory.BEAN_PFX + StorageEngine.rdbms.name(), factory),
				get(key, value));
		DBSessionManager dbMgr = new DBSessionManager(storageConfig);
		DataSource ds = dbMgr.getMyBatisConfiguration().getEnvironment().getDataSource();
		Assertions.assertThat(ds).isInstanceOf(PooledDataSource.class);
		return (PooledDataSource) ds;
	}

	private StoragePropertiesSource get(String key, String value)
	{
		return () ->
		{
			Properties ret = new Properties();
			ret.setProperty(key, value);
			return ret;
		};
	}

}
