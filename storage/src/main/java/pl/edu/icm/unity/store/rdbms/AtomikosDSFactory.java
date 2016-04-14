/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.atomikos.jdbc.AtomikosDataSourceBean;

import eu.unicore.util.db.DBPropertiesHelper;
import pl.edu.icm.unity.store.tx.AtomikosBeansFactory;

/**
 * Produces fundamental Atomikos components
 * @author K. Benedyczak
 */
@Configuration
public class AtomikosDSFactory
{
	private static final Map<String, String> XA_DS_IMPL = new HashMap<>();
	static 
	{
		XA_DS_IMPL.put("mysql", "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
		XA_DS_IMPL.put("h2", "org.h2.jdbcx.JdbcDataSource");
		XA_DS_IMPL.put("psql", "org.postgresql.xa.PGXADataSource");
	}
	
	@Autowired
	@Bean(destroyMethod="close")
	@DependsOn(AtomikosBeansFactory.USER_TX_MANAGER_BEAN)
	public AtomikosDataSourceBean getAtomikosDataSource(DBConfiguration config)
	{
		AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
		ds.setUniqueResourceName("rdbms-default");
		ds.setMaxPoolSize(config.getIntValue(DBConfiguration.MAX_POOL_SIZE));
		ds.setMinPoolSize(config.getIntValue(DBConfiguration.MIN_POOL_SIZE));
		ds.setMaxLifetime(config.getIntValue(DBConfiguration.MAX_IDLE_CONNECTION_TIME)); 
		ds.setXaDataSourceClassName(XA_DS_IMPL.get(config.getValue(DBPropertiesHelper.DIALECT)));
		Properties p = new Properties(); 
		p.setProperty("user", config.getValue(DBPropertiesHelper.USER)); 
		p.setProperty("password", config.getValue(DBPropertiesHelper.PASSWORD)); 
		p.setProperty("URL", config.getValue(DBPropertiesHelper.URL));
		ds.setXaProperties(p);
		return ds;
	}
}
