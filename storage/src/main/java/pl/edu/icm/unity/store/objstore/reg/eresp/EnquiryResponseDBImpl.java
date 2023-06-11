/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.eresp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.cred.CredentialDBImpl;
import pl.edu.icm.unity.store.objstore.reg.RequestCredentialChangeListener;

/**
 * Easy access to {@link EnquiryResponseState} storage.
 * 
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponseDBImpl extends GenericObjectsDAOImpl<EnquiryResponseState> 
		implements EnquiryResponseDB
{
	@Autowired
	public EnquiryResponseDBImpl(EnquiryResponseHandler handler, ObjectStoreDAO dbGeneric, 
			CredentialDBImpl credentialDB)
	{
		super(handler, dbGeneric, EnquiryResponseState.class, "enquiry response");
		credentialDB.addUpdateHandler(new RequestCredentialChangeListener<>(this));
	}
}
