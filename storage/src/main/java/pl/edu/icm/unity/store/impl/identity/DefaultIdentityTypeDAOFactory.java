/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identity;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.base.internal.StorageEngine;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;

/**
 * Produces a default {@link IdentityTypeDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultIdentityTypeDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public IdentityTypeDAO getDefaultIdentityTypeDAO(StorageConfiguration cfg, 
			Map<String, IdentityTypeDAO> daos)
	{
		StorageEngine engine = cfg.getEnumValue(StorageConfiguration.ENGINE, StorageEngine.class);
		return daos.get(IdentityTypeDAO.DAO_ID + engine.name());
	}
}
