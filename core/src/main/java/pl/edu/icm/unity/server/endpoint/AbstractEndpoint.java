/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import java.net.URL;
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
	protected URL baseUrl;
	
	public AbstractEndpoint(EndpointTypeDescription type)
	{
		description = new EndpointDescription();
		description.setType(type);
	}
	
	@Override
	public void initialize(String id, URL baseUrl, String contextAddress, String description, 
			List<AuthenticatorSet> authenticatorsInfo, List<Map<String, BindingAuthn>> authenticators,
			String serializedConfiguration)
	{
		this.description.setId(id);
		this.description.setDescription(description);
		this.description.setContextAddress(contextAddress);
		this.description.setAuthenticatorSets(authenticatorsInfo);
		this.authenticators = authenticators;
		this.baseUrl = baseUrl;
		setSerializedConfiguration(serializedConfiguration);
	}

	protected abstract void setSerializedConfiguration(String serializedState);
	
	@Override
	public EndpointDescription getEndpointDescription()
	{
		return description;
	}

	@Override
	public void destroy()
	{
	}
	
	/**
	 * @return the URL where the server listens to. It has no path element.
	 */
	public URL getBaseUrl()
	{
		return baseUrl;
	}
}
