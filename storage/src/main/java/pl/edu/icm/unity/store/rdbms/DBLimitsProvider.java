/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds DBLimit to Spring context
 * @author K. Benedyczak
 */
@Configuration
public class DBLimitsProvider
{
	@Bean
	@Autowired
	public DBLimit getDBLimits(DB db)
	{
		return db.getDBLimits();
	}
}
