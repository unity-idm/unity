/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.files;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.FileDAO;

/**
 * Produces a default {@link FileDAO}, basing on the configuration. 
 * @author P.Piernik
 */
@Configuration
public class DefaultFileDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public FileDAO getDefaultFileDAO(StorageConfiguration cfg, 
			Map<String, FileDAO> daos)
	{
		return daos.get(FileDAO.DAO_ID + cfg.getEngine().name());
	}
}
