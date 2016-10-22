/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;

/**
 * Produces a default {@link AttributeTypeDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultAttributeTypeDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public AttributeTypeDAOInternal getDefaultAttributeTypeDAO(StorageConfiguration cfg, 
			Map<String, AttributeTypeDAOInternal> atDAOs)
	{
		return atDAOs.get(AttributeTypeDAO.DAO_ID + cfg.getEngine().name());
	}
}
