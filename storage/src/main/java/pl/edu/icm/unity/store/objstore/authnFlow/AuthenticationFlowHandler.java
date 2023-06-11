/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.authnFlow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link AuthenticationFlowDefinition}
 * @author P.Piernik
 */
@Component
public class AuthenticationFlowHandler extends DefaultEntityHandler<AuthenticationFlowDefinition>
{
	public static final String AUTHENTICATION_FLOW_OBJECT_TYPE = "authenticationFlow";
	
	@Autowired
	public AuthenticationFlowHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, AUTHENTICATION_FLOW_OBJECT_TYPE, AuthenticationFlowDefinition.class);
	}

	@Override
	public GenericObjectBean toBlob(AuthenticationFlowDefinition value)
	{
		try
		{
			byte[] contents = jsonMapper.writeValueAsBytes(AuthenticationFlowMapper.map(value));
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize authentication flow JSON", e);
		}
	}

	@Override
	public AuthenticationFlowDefinition fromBlob(GenericObjectBean blob)
	{
		try
		{
			return AuthenticationFlowMapper.map(jsonMapper.readValue(blob.getContents(), DBAuthenticationFlow.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize authentication flow from JSON", e);
		}
	}
}
