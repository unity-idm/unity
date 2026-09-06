/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;

/**
 * Produces a default {@link AttributesCacheDAO}, basing on the configuration.
 */
@Configuration
public class DefaultAttributesCacheDAOFactory
{
	@Bean
	@Primary
	public AttributesCacheDAO getDefaultAttributesCacheDAO(StorageConfiguration cfg,
			Map<String, AttributesCacheDAO> daos)
	{
		return daos.get(AttributesCacheDAO.DAO_ID + cfg.getEngine().name());
	}
}
