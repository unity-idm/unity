/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.req;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
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
		return new GenericObjectBean(value.getName(), 
				JsonUtil.serialize2Bytes(value.toJson()), supportedType);
	}

	@Override
	public RegistrationRequestState fromBlob(GenericObjectBean blob)
	{
		return new RegistrationRequestState(JsonUtil.parse(blob.getContents()));
	}
}
