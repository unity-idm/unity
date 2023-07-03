/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.invite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

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
		try
		{
			return new GenericObjectBean(value.getName(),
					jsonMapper.writeValueAsBytes(InvitationWithCodeMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize invitation to JSON", e);
		}
	}

	@Override
	public InvitationWithCode fromBlob(GenericObjectBean blob)
	{
		try
		{
			return InvitationWithCodeMapper
					.map(jsonMapper.readValue(blob.getContents(), DBInvitationWithCode.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize invitation from JSON", e);
		}
	}
}
