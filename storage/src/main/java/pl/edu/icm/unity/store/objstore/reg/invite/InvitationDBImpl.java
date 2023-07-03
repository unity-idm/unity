/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.invite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;

/**
 * Easy access to {@link InvitationWithCode} storage.
 * 
 * @author K. Benedyczak
 */
@Component
public class InvitationDBImpl extends GenericObjectsDAOImpl<InvitationWithCode> 
		implements InvitationDB
{
	@Autowired
	public InvitationDBImpl(InvitationHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, InvitationWithCode.class, "invitation");
	}
}
