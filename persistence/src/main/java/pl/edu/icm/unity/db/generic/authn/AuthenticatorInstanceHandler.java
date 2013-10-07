/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.authn;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;

/**
 * Handler for {@link AuthenticatorInstance}
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorInstanceHandler extends DefaultEntityHandler<AuthenticatorInstance>
{
	public static final String AUTHENTICATOR_OBJECT_TYPE = "authenticator";
	
	@Autowired
	public AuthenticatorInstanceHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, AUTHENTICATOR_OBJECT_TYPE, AuthenticatorInstance.class);
	}

	@Override
	public GenericObjectBean toBlob(AuthenticatorInstance value, SqlSession sql)
	{
		try
		{
			byte[] contents = jsonMapper.writeValueAsBytes(value);
			return new GenericObjectBean(value.getId(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize authenticator state to JSON", e);
		}
	}

	@Override
	public AuthenticatorInstance fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			return jsonMapper.readValue(blob.getContents(), AuthenticatorInstance.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize authenticator state from JSON", e);
		}
	}
}
