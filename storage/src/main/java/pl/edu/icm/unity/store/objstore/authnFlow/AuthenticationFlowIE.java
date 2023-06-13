/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authnFlow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;

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
		super(dao, jsonMapper, 117, AuthenticationFlowHandler.AUTHENTICATION_FLOW_OBJECT_TYPE);
	}

	@Override
	protected AuthenticationFlowDefinition convert(ObjectNode src)
	{
		return AuthenticationFlowMapper.map(jsonMapper.convertValue(src, DBAuthenticationFlow.class));
	}

	@Override
	protected ObjectNode convert(AuthenticationFlowDefinition src)
	{
		return jsonMapper.convertValue(AuthenticationFlowMapper.map(src), ObjectNode.class);
	}
}



