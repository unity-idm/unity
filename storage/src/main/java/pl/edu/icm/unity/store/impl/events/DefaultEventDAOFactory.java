/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.events;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.EventDAO;

/**
 * Produces a default {@link EventDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultEventDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public EventDAO getDefaultEventDAO(StorageConfiguration cfg, 
			Map<String, EventDAO> daos)
	{
		return daos.get(EventDAO.DAO_ID + cfg.getEngine().name());
	}
}
