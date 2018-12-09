/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;

/**
 * Handles import/export of {@link AuthenticatorConfiguration}.
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorConfigurationIE extends GenericObjectIEBase<AuthenticatorConfiguration>
{
	@Autowired
	public AuthenticatorConfigurationIE(AuthenticatorConfigurationDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, AuthenticatorConfiguration.class, 101, 
				AuthenticatorConfigurationHandler.AUTHENTICATOR_OBJECT_TYPE);
	}
}



