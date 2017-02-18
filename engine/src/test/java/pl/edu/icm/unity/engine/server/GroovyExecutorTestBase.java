/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.api.initializers.ContentInitConf;

public class GroovyExecutorTestBase
{
	protected static final int LOAD_CREDENTIALS_FROM_CONFIGURATION = 0;
	@Autowired
	protected ContentInitializersExecutor config;
	@Autowired
	protected ContentGroovyExecutor groovyExecutor;
	
	protected ContentInitConf getConf(int index)
	{
		return config.getContentInitializersConfiguration().get(index);
	}
	
	
}
