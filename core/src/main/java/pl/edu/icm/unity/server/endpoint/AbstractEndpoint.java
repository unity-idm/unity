/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
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
	protected Properties properties;
	
	public AbstractEndpoint(EndpointTypeDescription type)
	{
		description = new EndpointDescription();
		description.setType(type);
	}
	
	@Override
	public synchronized void initialize(String id, I18nString displayedName, 
			URL baseUrl, String contextAddress, String description, 
			List<AuthenticatorSet> authenticatorsInfo, List<Map<String, BindingAuthn>> authenticators,
			AuthenticationRealm realm, String serializedConfiguration)
	{
		this.description.setId(id);
		this.description.setDisplayedName(displayedName);
		this.description.setDescription(description);
		this.description.setContextAddress(contextAddress);
		this.description.setAuthenticatorSets(authenticatorsInfo);
		this.description.setRealm(realm);
		this.authenticators = authenticators;
		this.baseUrl = baseUrl;
		setSerializedConfiguration(serializedConfiguration);
	}

	@Override
	public String getSerializedConfiguration()
	{
		CharArrayWriter writer = new CharArrayWriter();
		try
		{
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new IllegalStateException("Can not serialize endpoint's configuration", e);
		}
		return writer.toString();
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
	
	@Override
	public synchronized List<Map<String, BindingAuthn>> getAuthenticators()
	{
		return authenticators;
	}

	protected synchronized void setAuthenticators(List<Map<String, BindingAuthn>> authenticators)
	{
		this.authenticators = new ArrayList<>(authenticators);
	}
	
	/**
	 * @return the URL where the server listens to. It has no path element.
	 */
	public URL getBaseUrl()
	{
		return baseUrl;
	}
	
	/**
	 * This method makes sense only for the {@link WebAppEndpointInstance}s.
	 * 
	 * @param servletPath path of the servlet exposing the endpoint, Only the servlet's path, without context prefix.
	 * @return URL in string form, including the servers address, context address and 
	 * the servlet's address. 
	 */
	public String getServletUrl(String servletPath)
	{
		return getBaseUrl().toExternalForm() +
				getEndpointDescription().getContextAddress() + 
				servletPath;
	}
}
