/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.req;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.cred.CredentialDBImpl;
import pl.edu.icm.unity.store.objstore.reg.RequestCredentialChangeListener;

/**
 * Easy access to {@link RegistrationRequestState} storage.
 * 
 * @author K. Benedyczak
 */
@Component
public class RegistrationRequestDBImpl extends GenericObjectsDAOImpl<RegistrationRequestState> 
		implements RegistrationRequestDB
{
	@Autowired
	public RegistrationRequestDBImpl(RegistrationRequestHandler handler, ObjectStoreDAO dbGeneric, 
			CredentialDBImpl credentialDB)
	{
		super(handler, dbGeneric, RegistrationRequestState.class, "registration request");
		credentialDB.addUpdateHandler(new RequestCredentialChangeListener<>(this));
	}
}
