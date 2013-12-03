/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.db.generic.cred.CredentialHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.LocalCredentialsRegistry;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 * Easy access to {@link RegistrationRequestState} storage.
 * <p>
 * Note - it is more effective to implement consistency checking in the manager object,
 * and it is done there.
 * @author K. Benedyczak
 */
@Component
public class RegistrationRequestDB extends GenericObjectsDB<RegistrationRequestState>
{
	private LocalCredentialsRegistry authnRegistry;
	private CredentialDB credentialDB;
	
	@Autowired
	public RegistrationRequestDB(RegistrationRequestHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager,
			LocalCredentialsRegistry authnRegistry, CredentialDB credentialDB)
	{
		super(handler, dbGeneric, notificationManager, RegistrationRequestState.class,
				"registration form");
		this.authnRegistry = authnRegistry;
		this.credentialDB = credentialDB;
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
		public void preUpdate(CredentialDefinition oldObject, CredentialDefinition updatedObject, 
				SqlSession sql) throws EngineException 
		{
			List<RegistrationRequestState> requests = getAll(sql);
			for (RegistrationRequestState req: requests)
			{
				if (req.getStatus() != RegistrationRequestStatus.pending)
					continue;
				for (CredentialParamValue crParam: req.getRequest().getCredentials())
				{
					if (updatedObject.getName().equals(crParam.getCredentialId()))
					{
						validateRequestCredential(crParam.getCredentialId(), 
								crParam.getSecrets(), sql);
					}
				}
			}
		}

		
		public void preRemove(CredentialDefinition removedObject, SqlSession sql)
				throws EngineException
		{
			//removal is protected by the form
		}
	}

	private void validateRequestCredential(String credential, String secrets, SqlSession sql) 
			throws EngineException
	{
		CredentialDefinition credDef = credentialDB.get(credential, sql);
		LocalCredentialVerificator credVerificator = authnRegistry.createLocalCredentialVerificator(credDef);
		credVerificator.prepareCredential(secrets, "");
	}

}
