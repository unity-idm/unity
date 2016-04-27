/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.GroupDAO;

/**
 * Produces a default {@link GroupDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultGroupDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public GroupDAO getDefaultGroupDAO(StorageConfiguration cfg, Map<String, GroupDAO> daos)
	{
		return daos.get(GroupDAO.DAO_ID + cfg.getEngine().name());
	}
}
