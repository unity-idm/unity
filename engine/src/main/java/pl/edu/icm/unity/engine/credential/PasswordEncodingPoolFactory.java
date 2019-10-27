/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.concurrent.ForkJoinPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.credential.pass.PasswordEncodingPoolProvider;

@Configuration
class PasswordEncodingPoolFactory
{
	private UnityServerConfiguration config;
	
	@Autowired
	public PasswordEncodingPoolFactory(UnityServerConfiguration config)
	{
		this.config = config;
	}

	@Bean
	public PasswordEncodingPoolProvider getPasswordEncodingPoolProvider()
	{
		return new PasswordEncodingPoolProvider(new ForkJoinPool(config.getMaxConcurrentPasswordChecks()));
	}
}
