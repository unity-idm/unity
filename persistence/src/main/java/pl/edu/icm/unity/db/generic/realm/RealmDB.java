/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.realm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Easy access to {@link AuthenticationRealm} storage.
 * @author K. Benedyczak
 */
@Component
public class RealmDB  extends GenericObjectsDB<AuthenticationRealm>
{
	@Autowired
	public RealmDB(RealmHandler handler, DBGeneric dbGeneric,
			DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, AuthenticationRealm.class, "authentication realm");
	}
}
