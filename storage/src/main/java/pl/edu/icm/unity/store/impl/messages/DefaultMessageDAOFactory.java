/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.messages;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.MessagesDAO;

/**
 * Produces a default {@link MessagesDAO}, basing on the configuration. 
 * @author P.Piernik
 */
@Configuration
public class DefaultMessageDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public MessagesDAO getDefaultMessageDAO(StorageConfiguration cfg, 
			Map<String, MessagesDAO> daos)
	{
		return daos.get(MessagesDAO.DAO_ID + cfg.getEngine().name());
	}
}
