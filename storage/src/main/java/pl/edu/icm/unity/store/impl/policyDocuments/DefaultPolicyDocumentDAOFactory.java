/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.policyDocuments;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.PolicyDocumentDAO;

/**
 * Produces a default {@link PolicyDocumentDAO}, basing on the configuration.
 * 
 * @author P.Piernik
 */
@Configuration
public class DefaultPolicyDocumentDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public PolicyDocumentDAO getDefaultPolicyDocumentsDAO(StorageConfiguration cfg, Map<String, PolicyDocumentDAO> daos)
	{
		return daos.get(PolicyDocumentDAO.DAO_ID + cfg.getEngine().name());
	}
}
