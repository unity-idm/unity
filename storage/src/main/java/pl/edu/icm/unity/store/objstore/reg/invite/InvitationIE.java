/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.invite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles import/export of {@link InvitationWithCode}.
 * @author K. Benedyczak
 */
@Component
public class InvitationIE extends GenericObjectIEBase<InvitationWithCode>
{
	@Autowired
	public InvitationIE(InvitationDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, InvitationWithCode.class, 116, 
				InvitationHandler.INVITATION_OBJECT_TYPE);
	}
}



