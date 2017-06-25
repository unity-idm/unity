/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.AttributeDAO;

/**
 * Produces a default {@link AttributeDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultAttributeDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public AttributeDAO getDefaultAttributeDAO(StorageConfiguration cfg, 
			Map<String, AttributeDAO> daos)
	{
		return daos.get(AttributeDAO.DAO_ID + cfg.getEngine().name());
	}
}
