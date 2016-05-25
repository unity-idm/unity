/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.List;

import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;

public class RequestCredentialChangeListener<T extends UserRequestState<?>> 
		implements ReferenceUpdateHandler<CredentialDefinition>
{
	private NamedCRUDDAOWithTS<T> dao;
	
	public RequestCredentialChangeListener(NamedCRUDDAOWithTS<T> dao)
	{
		this.dao = dao;
	}

	@Override
	public void preUpdateCheck(long modifiedId, String modifiedName,
			CredentialDefinition newValue)
	{
		List<T> requests = dao.getAll();
		for (UserRequestState<?> req: requests)
		{
			if (req.getStatus() != RegistrationRequestStatus.pending)
				continue;
			for (CredentialParamValue crParam: req.getRequest().getCredentials())
			{
				if (modifiedName.equals(crParam.getCredentialId()))
					throw new IllegalArgumentException("The credential "
							+ "is used by a pending registration request and "
							+ "can not be modified: " + req.getName());
			}
		}
	}
}