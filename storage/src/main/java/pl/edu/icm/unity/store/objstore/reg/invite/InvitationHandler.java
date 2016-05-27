/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.invite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handler for {@link InvitationWithCode}s storage.
 * @author K. Benedyczak
 */
@Component
public class InvitationHandler extends DefaultEntityHandler<InvitationWithCode>
{
	public static final String INVITATION_OBJECT_TYPE = "invitationWithCode";
	
	@Autowired
	public InvitationHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, INVITATION_OBJECT_TYPE, InvitationWithCode.class);
	}

	@Override
	public GenericObjectBean toBlob(InvitationWithCode value)
	{
		return new GenericObjectBean(value.getName(), 
				JsonUtil.serialize2Bytes(value.toJson()), supportedType);
	}

	@Override
	public InvitationWithCode fromBlob(GenericObjectBean blob)
	{
		return new InvitationWithCode(JsonUtil.parse(blob.getContents()));
	}
}
