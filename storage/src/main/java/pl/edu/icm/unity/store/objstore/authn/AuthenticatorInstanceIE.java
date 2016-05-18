/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;

/**
 * Handles import/export of {@link AuthenticatorInstance}.
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorInstanceIE extends GenericObjectIEBase<AuthenticatorInstance>
{
	@Autowired
	public AuthenticatorInstanceIE(AuthenticatorInstanceDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, AuthenticatorInstance.class, 101, "authenticatorInstance");
	}
}



