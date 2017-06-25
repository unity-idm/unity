/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.realm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

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
		return new GenericObjectBean(value.getName(), 
				JsonUtil.serialize2Bytes(value.toJson()), supportedType);
	}

	@Override
	public AuthenticationRealm fromBlob(GenericObjectBean blob)
	{
		return new AuthenticationRealm(JsonUtil.parse(blob.getContents()));
	}
}
