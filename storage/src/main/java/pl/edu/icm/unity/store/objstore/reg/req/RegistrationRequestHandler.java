/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.req;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Handler for {@link RegistrationRequestState}s storage.
 * @author K. Benedyczak
 */
@Component
public class RegistrationRequestHandler extends DefaultEntityHandler<RegistrationRequestState>
{
	public static final String REGISTRATION_REQUEST_OBJECT_TYPE = "registrationRequest";
	
	@Autowired
	public RegistrationRequestHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, REGISTRATION_REQUEST_OBJECT_TYPE, RegistrationRequestState.class);
	}
	
	@Override
	public GenericObjectBean toBlob(RegistrationRequestState value)
	{
		try
		{
			return new GenericObjectBean(value.getName(),
					jsonMapper.writeValueAsBytes(RegistrationRequestStateMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize registration request to JSON", e);
		}
	}

	@Override
	public RegistrationRequestState fromBlob(GenericObjectBean blob)
	{
		try
		{
			return RegistrationRequestStateMapper.map(jsonMapper.readValue(blob.getContents(), DBRegistrationRequestState.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration request from JSON", e);
		}
	}
}
