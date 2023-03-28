/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.realm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase2;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Handles import/export of {@link AuthenticationRealm}.
 * @author K. Benedyczak
 */
@Component
public class RealmIE extends GenericObjectIEBase2<AuthenticationRealm>
{
	@Autowired
	public RealmIE(RealmDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 107, RealmHandler.REALM_OBJECT_TYPE);
	}
	
	@Override
	protected AuthenticationRealm convert(ObjectNode src)
	{
		return AuthenticationRealmMapper.map(jsonMapper.convertValue(src, DBAuthenticationRealm.class));
	}

	@Override
	protected ObjectNode convert(AuthenticationRealm src)
	{
		return jsonMapper.convertValue(AuthenticationRealmMapper.map(src), ObjectNode.class);
	}
}



