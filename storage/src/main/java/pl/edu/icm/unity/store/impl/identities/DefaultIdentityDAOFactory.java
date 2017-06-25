/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.IdentityDAO;

/**
 * Produces a default {@link IdentityDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultIdentityDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public IdentityDAO getDefaultIdentityDAO(StorageConfiguration cfg, Map<String, IdentityDAO> daos)
	{
		return daos.get(IdentityDAO.DAO_ID + cfg.getEngine().name());
	}
}
