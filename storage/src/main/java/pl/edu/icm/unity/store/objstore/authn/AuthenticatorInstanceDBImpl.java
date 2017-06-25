/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authn;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.cred.CredentialDBImpl;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Easy access to AuthenticatorInstance storage.
 * <p>
 * Adds consistency checking: credential can not be removed when is used by an authenticator. 
 * Adds update checking: when credential is updated, authenticator modification TS is updated too,
 * so the system can detect this and reconfigure affected endpoints.
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorInstanceDBImpl extends GenericObjectsDAOImpl<AuthenticatorInstance> 
		implements AuthenticatorInstanceDB
{
	@Autowired
	public AuthenticatorInstanceDBImpl(AuthenticatorInstanceHandler handler,
			ObjectStoreDAO dbGeneric, CredentialDBImpl credentialDB)
	{
		super(handler, dbGeneric, AuthenticatorInstance.class, "authenticator");
		credentialDB.addRemovalHandler(this::restrictCredentialRemoval);
		credentialDB.addUpdateHandler(this::credentialUpdateChangesOurTS);
	}
	
	private void credentialUpdateChangesOurTS(long modifiedId, String modifiedName, CredentialDefinition newValue)
	{
		List<AuthenticatorInstance> auths = getAll();
		for (AuthenticatorInstance authenticator: auths)
		{
			if (modifiedName.equals(authenticator.getLocalCredentialName()))
				updateTS(authenticator.getId());
		}
	}
	
	private void restrictCredentialRemoval(long removedId, String removedName)
	{
		List<AuthenticatorInstance> auths = getAll();
		for (AuthenticatorInstance authenticator: auths)
		{
			if (removedName.equals(authenticator.getLocalCredentialName()))
				throw new IllegalArgumentException("The credential is used by an authenticator " 
						+ authenticator.getId());
		}
	}
}
