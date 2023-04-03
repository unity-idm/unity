/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.invite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

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
		super(dao, jsonMapper, 116, 
				InvitationHandler.INVITATION_OBJECT_TYPE);
	}
	
	@Override
	protected InvitationWithCode convert(ObjectNode src)
	{
		return InvitationWithCodeMapper.map(jsonMapper.convertValue(src, DBInvitationWithCode.class));
	}

	@Override
	protected ObjectNode convert(InvitationWithCode src)
	{
		return jsonMapper.convertValue(InvitationWithCodeMapper.map(src), ObjectNode.class);
	}
}



