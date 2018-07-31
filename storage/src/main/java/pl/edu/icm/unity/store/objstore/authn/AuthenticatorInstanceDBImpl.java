/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.cred.CredentialDBImpl;
import pl.edu.icm.unity.store.rdbms.cache.HashMapNamedCache;
import pl.edu.icm.unity.store.rdbms.cache.NamedCachingCRUDWithTS;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;

/**
 * Easy access to AuthenticatorInstance storage.
 * <p>
 * Adds consistency checking: credential can not be removed when is used by an authenticator. 
 * Adds update checking: when credential is updated, authenticator modification TS is updated too,
 * so the system can detect this and reconfigure affected endpoints.
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorInstanceDBImpl extends NamedCachingCRUDWithTS<AuthenticatorInstance, AuthenticatorInstanceDBNoChacheImpl> 
		implements AuthenticatorInstanceDB
{
	@Autowired
	public AuthenticatorInstanceDBImpl(AuthenticatorInstanceHandler handler,
			ObjectStoreDAO dbGeneric, CredentialDBImpl credentialDB)
	{
		super(new AuthenticatorInstanceDBNoChacheImpl(handler, dbGeneric, credentialDB), 
				new HashMapNamedCache<>(a -> a.clone()));
	}
}
