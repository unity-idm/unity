/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.generic.DependencyChangeListener;
import pl.edu.icm.unity.db.generic.cred.CredentialHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;

public class RequestCredentialChangeListener implements DependencyChangeListener<CredentialDefinition>
{
	private RequestsSupplier supplier;
	
	public RequestCredentialChangeListener(RequestsSupplier supplier)
	{
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
					throw new SchemaConsistencyException("The modified credential is used"
							+ "in a PENDING registration or enquiry request. "
							+ "Please process all the requests using this credential "
							+ "first and only then modify its settings. "
							+ "Offending request: " + req.getRequestId());
			}
		}
	}

	
	public void preRemove(CredentialDefinition removedObject, SqlSession sql)
			throws EngineException
	{
		//removal is protected by the form
	}
}