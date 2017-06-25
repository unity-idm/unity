/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;

/**
 * Produces a default {@link ObjectStoreDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultObjectStoreDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public ObjectStoreDAO getDefaultObjectStoreDAO(StorageConfiguration cfg, 
			Map<String, ObjectStoreDAO> daos)
	{
		return daos.get(ObjectStoreDAO.DAO_ID + cfg.getEngine().name());
	}
}
