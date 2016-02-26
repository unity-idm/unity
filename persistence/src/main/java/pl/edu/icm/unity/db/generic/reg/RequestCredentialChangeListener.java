/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.db.generic.cred.CredentialHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.LocalCredentialsRegistry;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;

public class RequestCredentialChangeListener implements DependencyChangeListener<CredentialDefinition>
{
	private LocalCredentialsRegistry authnRegistry;
	private CredentialDB credentialDB;
	private RequestsSupplier supplier;
	
	public RequestCredentialChangeListener(LocalCredentialsRegistry authnRegistry,
			CredentialDB credentialDB, RequestsSupplier supplier)
	{
		this.authnRegistry = authnRegistry;
		this.credentialDB = credentialDB;
		this.supplier = supplier;
	}

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
		List<? extends UserRequestState<?>> requests = supplier.getRequests(sql);
		for (UserRequestState<?> req: requests)
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
	
	private void validateRequestCredential(String credential, String secrets, SqlSession sql) 
			throws EngineException
	{
		CredentialDefinition credDef = credentialDB.get(credential, sql);
		LocalCredentialVerificator credVerificator = authnRegistry.createLocalCredentialVerificator(credDef);
		credVerificator.prepareCredential(secrets, "");
	}
}