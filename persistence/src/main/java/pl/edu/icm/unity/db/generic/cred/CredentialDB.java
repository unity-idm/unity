/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.cred;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Easy access to {@link CredentialDefinition} storage.
 * @author K. Benedyczak
 */
@Component
public class CredentialDB extends GenericObjectsDB<CredentialDefinition>
{
	@Autowired
	public CredentialDB(CredentialHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, CredentialDefinition.class,
				"credential");
	}
}
