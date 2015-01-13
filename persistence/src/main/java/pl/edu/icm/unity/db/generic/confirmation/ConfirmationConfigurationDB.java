/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.confirmation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;

/**
 * Easy to use interface to {@link ConfirmationConfiguration} storage.
 *  
 * @author P. Piernik
 */
@Component
public class ConfirmationConfigurationDB extends GenericObjectsDB<ConfirmationConfiguration>
{
	@Autowired
	public ConfirmationConfigurationDB(ConfirmationConfigurationHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, ConfirmationConfiguration.class, "confirmation configuration");
	}

}
