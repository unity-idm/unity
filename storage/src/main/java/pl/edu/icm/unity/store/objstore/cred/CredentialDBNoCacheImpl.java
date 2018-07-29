/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.cred;

import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Easy access to {@link CredentialDefinition} storage.
 * @author K. Benedyczak
 */
class CredentialDBNoCacheImpl extends GenericObjectsDAOImpl<CredentialDefinition> implements CredentialDB
{
	CredentialDBNoCacheImpl(CredentialHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, CredentialDefinition.class, "credential");
	}
}
