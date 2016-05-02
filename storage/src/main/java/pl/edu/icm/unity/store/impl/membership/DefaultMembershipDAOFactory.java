/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.MembershipDAO;

/**
 * Produces a default {@link MembershipDAO}, basing on the configuration. 
 * @author K. Benedyczak
 */
@Configuration
public class DefaultMembershipDAOFactory
{
	@Bean
	@Primary
	@Autowired
	public MembershipDAO getDefaultMembershipDAO(StorageConfiguration cfg, 
			Map<String, MembershipDAO> daos)
	{
		return daos.get(MembershipDAO.DAO_ID + cfg.getEngine().name());
	}
}
