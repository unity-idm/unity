/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.EntityDAO;

/**
 * Produces a default {@link EntityDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultEntityTypeDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public EntityDAO getDefaultEntityDAO(StorageConfiguration cfg, 
			Map<String, EntityDAO> daos)
	{
		return daos.get(EntityDAO.DAO_ID + cfg.getEngine().name());
	}
}
