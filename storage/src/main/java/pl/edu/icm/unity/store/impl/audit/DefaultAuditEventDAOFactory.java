/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.AuditEventDAO;

import java.util.Map;

/**
 * Produces a default {@link AuditEventDAO}, basing on the configuration.
 * @author R. Ledzinski
 */
@Configuration
public class DefaultAuditEventDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public AuditEventDAO getDefaultAuditEventDAO(StorageConfiguration cfg,
												 Map<String, AuditEventDAO> daos)
	{
		return daos.get(AuditEventDAO.DAO_ID + cfg.getEngine().name());
	}
}
