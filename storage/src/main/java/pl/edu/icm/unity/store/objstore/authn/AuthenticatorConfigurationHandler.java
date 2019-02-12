/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;

/**
 * Handler for {@link AuthenticatorConfiguration}
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorConfigurationHandler extends DefaultEntityHandler<AuthenticatorConfiguration>
{
	public static final String AUTHENTICATOR_OBJECT_TYPE = "authenticator";
	
	@Autowired
	public AuthenticatorConfigurationHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, AUTHENTICATOR_OBJECT_TYPE, AuthenticatorConfiguration.class);
	}

	@Override
	public GenericObjectBean toBlob(AuthenticatorConfiguration value)
	{
		try
		{
			byte[] contents = jsonMapper.writeValueAsBytes(value);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize authenticator state to JSON", e);
		}
	}

	@Override
	public AuthenticatorConfiguration fromBlob(GenericObjectBean blob)
	{
		try
		{
			return jsonMapper.readValue(blob.getContents(), AuthenticatorConfiguration.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize authenticator state from JSON", e);
		}
	}
}
