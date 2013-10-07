/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.reg;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Handler for {@link RegistrationRequestState}
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
	public GenericObjectBean toBlob(RegistrationRequestState value, SqlSession sql)
	{
		try
		{
			byte[] contents = jsonMapper.writeValueAsBytes(value);
			return new GenericObjectBean(value.getRequestId(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize registration form to JSON", e);
		}
	}

	@Override
	public RegistrationRequestState fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			return jsonMapper.readValue(blob.getContents(), RegistrationRequestState.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration form from JSON", e);
		}
	}
}
