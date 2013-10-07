/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.authn;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.db.generic.cred.CredentialHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
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
public class AuthenticatorInstanceDB extends GenericObjectsDB<AuthenticatorInstance>
{
	@Autowired
	public AuthenticatorInstanceDB(AuthenticatorInstanceHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
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
		public void preAdd(CredentialDefinition newObject, SqlSession sql) throws EngineException { }

		@Override
		public void preUpdate(CredentialDefinition oldObject,
				CredentialDefinition updatedObject, SqlSession sql) throws EngineException 
		{
			List<AuthenticatorInstance> auths = getAll(sql);
			for (AuthenticatorInstance authenticator: auths)
			{
				if (updatedObject.getName().equals(authenticator.getLocalCredentialName()))
				{
					updateTS(authenticator.getId(), sql);
				}
			}
		}

		@Override
		public void preRemove(CredentialDefinition removedObject, SqlSession sql)
				throws EngineException
		{
			List<AuthenticatorInstance> auths = getAll(sql);
			for (AuthenticatorInstance authenticator: auths)
			{
				if (removedObject.getName().equals(authenticator.getLocalCredentialName()))
					throw new IllegalCredentialException("The credential is used by an authenticator " 
							+ authenticator.getId());
			}
		}
	}
}
