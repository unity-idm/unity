/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.realm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;

/**
 * Easy access to {@link AuthenticationRealm} storage.
 * @author K. Benedyczak
 */
@Component
public class RealmDBImpl extends GenericObjectsDAOImpl<AuthenticationRealm> implements RealmDB
{
	@Autowired
	public RealmDBImpl(RealmHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, AuthenticationRealm.class, "authentication realm");
	}
}
