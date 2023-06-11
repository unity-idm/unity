/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.realm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link AuthenticationRealm}s storage.
 * @author K. Benedyczak
 */
@Component
public class RealmHandler extends DefaultEntityHandler<AuthenticationRealm>
{
	public static final String REALM_OBJECT_TYPE = "authenticationRealm";
	
	@Autowired
	public RealmHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, REALM_OBJECT_TYPE, AuthenticationRealm.class);
	}	
	
	@Override
	public GenericObjectBean toBlob(AuthenticationRealm value)
	{
		try
		{
			return new GenericObjectBean(value.getName(),
					jsonMapper.writeValueAsBytes(AuthenticationRealmMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize authentiaction realm to JSON", e);
		}
	}

	@Override
	public AuthenticationRealm fromBlob(GenericObjectBean blob)
	{
		try
		{
			return AuthenticationRealmMapper
					.map(jsonMapper.readValue(blob.getContents(), DBAuthenticationRealm.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize authentiaction realm from JSON", e);
		}
	}
}
