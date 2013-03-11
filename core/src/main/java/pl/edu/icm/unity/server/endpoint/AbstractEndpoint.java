/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Typical boilerplate for all endpoints.
 * @author K. Benedyczak
 */
public abstract class AbstractEndpoint implements EndpointInstance
{
	protected EndpointDescription description;
	protected List<Map<String, BindingAuthn>> authenticators;
	
	public AbstractEndpoint(EndpointTypeDescription type)
	{
		description = new EndpointDescription();
		description.setType(type);
	}
	
	@Override
	public void initialize(String id, String contextAddress, String description, 
			List<AuthenticatorSet> authenticatorsInfo, List<Map<String, BindingAuthn>> authenticators)
	{
		this.description.setId(id);
		this.description.setDescription(description);
		this.description.setContextAddress(contextAddress);
		this.description.setAuthenticatorSets(authenticatorsInfo);
		this.authenticators = authenticators;
	}

	@Override
	public EndpointDescription getEndpointDescription()
	{
		return description;
	}

	@Override
	public void destroy()
	{
	}
}
