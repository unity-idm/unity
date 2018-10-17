/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authnFlow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;

/**
 * Handles import/export of {@link AuthenticationFlowDefinition}.
 * @author P.Piernik
 */
@Component
public class AuthenticationFlowIE extends GenericObjectIEBase<AuthenticationFlowDefinition>
{
	@Autowired
	public AuthenticationFlowIE(AuthenticationFlowDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, AuthenticationFlowDefinition.class, 117, AuthenticationFlowHandler.AUTHENTICATION_FLOW_OBJECT_TYPE);
	}
}



