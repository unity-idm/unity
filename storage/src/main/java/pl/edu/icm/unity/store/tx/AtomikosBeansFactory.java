/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

/**
 * Setup of Atomikos transactions support beans 
 * @author K. Benedyczak
 */
@Configuration
public class AtomikosBeansFactory
{
	@Bean(name="userTransactionService", destroyMethod="shutdownForce")
	public UserTransactionServiceImp getUserTransationServiceImp()
	{
		Properties props = new Properties();
		props.setProperty("com.atomikos.icatch.service",
	              "com.atomikos.icatch.standalone.UserTransactionServiceFactory");
		UserTransactionServiceImp ret = new UserTransactionServiceImp(props);
		ret.init();
		return ret;
	}
	
	@Bean(name="AtomikosTransactionManager", destroyMethod="close")
	@Autowired
	public UserTransactionManager getUserTransactionManager(UserTransactionServiceImp dependency)
	{
		UserTransactionManager ret = new UserTransactionManager();
		ret.setForceShutdown(true);
		ret.setStartupTransactionService(false);
		return ret;
	}
}
