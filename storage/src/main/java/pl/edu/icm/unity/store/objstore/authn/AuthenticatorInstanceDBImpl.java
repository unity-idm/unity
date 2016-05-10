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
import pl.edu.icm.unity.store.objstore.DependencyChangeListener;
import pl.edu.icm.unity.store.objstore.DependencyNotificationManager;
import pl.edu.icm.unity.store.objstore.GenericObjectsDB;
import pl.edu.icm.unity.store.objstore.cred.CredentialHandler;
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
public class AuthenticatorInstanceDBImpl extends GenericObjectsDB<AuthenticatorInstance> 
		implements AuthenticatorInstanceDB
{
	@Autowired
	public AuthenticatorInstanceDBImpl(AuthenticatorInstanceHandler handler,
			ObjectStoreDAO dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, AuthenticatorInstance.class,
				"authenticator");
		notificationManager.addListener(new CredentialChangeListener());
	}
	
	private class CredentialChangeListener implements DependencyChangeListener<CredentialDefinition>
	{
		@Override
		public String getDependencyObjectType()
		{
			return CredentialHandler.CREDENTIAL_OBJECT_TYPE;
		}

		@Override
		public void preAdd(CredentialDefinition newObject) { }

		@Override
		public void preUpdate(CredentialDefinition oldObject,
				CredentialDefinition updatedObject)
		{
			List<AuthenticatorInstance> auths = getAll();
			for (AuthenticatorInstance authenticator: auths)
			{
				if (updatedObject.getName().equals(authenticator.getLocalCredentialName()))
				{
					updateTS(authenticator.getId());
				}
			}
		}

		@Override
		public void preRemove(CredentialDefinition removedObject)
		{
			List<AuthenticatorInstance> auths = getAll();
			for (AuthenticatorInstance authenticator: auths)
			{
				if (removedObject.getName().equals(authenticator.getLocalCredentialName()))
					throw new IllegalArgumentException("The credential is used by an authenticator " 
							+ authenticator.getId());
			}
		}
	}
}
