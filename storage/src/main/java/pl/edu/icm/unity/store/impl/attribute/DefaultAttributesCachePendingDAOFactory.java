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
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;

/**
 * Produces a default {@link AttributesCachePendingDAO}, basing on the configuration.
 */
@Configuration
public class DefaultAttributesCachePendingDAOFactory
{
	@Bean
	@Primary
	public AttributesCachePendingDAO getDefaultAttributesCachePendingDAO(StorageConfiguration cfg,
			Map<String, AttributesCachePendingDAO> daos)
	{
		return daos.get(AttributesCachePendingDAO.DAO_ID + cfg.getEngine().name());
	}
}
