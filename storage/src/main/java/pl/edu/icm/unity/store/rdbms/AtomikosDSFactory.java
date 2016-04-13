/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

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
	@Autowired
	@Bean(destroyMethod="close")
	@DependsOn(AtomikosBeansFactory.USER_TX_MANAGER_BEAN)
	public AtomikosDataSourceBean getAtomikosDataSource(DBConfiguration config)
	{
		//TODO make this configurable
		AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
		ds.setUniqueResourceName("rdbms-default");
		ds.setMaxPoolSize(20);
		ds.setMinPoolSize(5);
		ds.setMaxLifetime(60*120); 
		//FIXME
		ds.setXaDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
		Properties p = new Properties(); 
		p.setProperty("user", config.getValue(DBPropertiesHelper.USER)); 
		p.setProperty("password", config.getValue(DBPropertiesHelper.PASSWORD)); 
		p.setProperty("URL", config.getValue(DBPropertiesHelper.URL));
		ds.setXaProperties(p);
		return ds;
	}
}
