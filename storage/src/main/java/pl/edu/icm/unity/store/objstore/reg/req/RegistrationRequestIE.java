/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.req;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Handles import/export of {@link RegistrationRequestState}.
 * @author K. Benedyczak
 */
@Component
public class RegistrationRequestIE extends GenericObjectIEBase<RegistrationRequestState>
{
	@Autowired
	public RegistrationRequestIE(RegistrationRequestDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 114, 
				RegistrationRequestHandler.REGISTRATION_REQUEST_OBJECT_TYPE);
	}
	
	@Override
	protected RegistrationRequestState convert(ObjectNode src)
	{
		return RegistrationRequestStateMapper.map(jsonMapper.convertValue(src, DBRegistrationRequestState.class));
	}

	@Override
	protected ObjectNode convert(RegistrationRequestState src)
	{
		return jsonMapper.convertValue(RegistrationRequestStateMapper.map(src), ObjectNode.class);
	}
}



