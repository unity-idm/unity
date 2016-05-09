/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.tokens;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.TokenDAO;

/**
 * Produces a default {@link TokenDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultTokenDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public TokenDAO getDefaultTokenDAO(StorageConfiguration cfg, 
			Map<String, TokenDAO> daos)
	{
		return daos.get(TokenDAO.DAO_ID + cfg.getEngine().name());
	}
}
