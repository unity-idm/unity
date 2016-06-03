/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.confirmation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.ConfirmationConfigurationDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.confirmation.ConfirmationConfiguration;

/**
 * Handles import/export of {@link ConfirmationConfiguration}.
 * @author K. Benedyczak
 */
@Component
public class ConfirmationConfigurationIE extends GenericObjectIEBase<ConfirmationConfiguration>
{
	@Autowired
	public ConfirmationConfigurationIE(ConfirmationConfigurationDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, ConfirmationConfiguration.class, 102, "confirmationConfiguration");
	}
}



