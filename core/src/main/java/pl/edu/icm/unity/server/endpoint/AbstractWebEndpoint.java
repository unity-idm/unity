/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import java.net.URL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.NetworkServer;

/**
 * Typical boilerplate for all {@link WebAppEndpointInstance}s.
 * @author K. Benedyczak
 */
public abstract class AbstractWebEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	protected NetworkServer httpServer;
	
	public AbstractWebEndpoint(NetworkServer httpServer)
	{
		this.httpServer = httpServer;
	}

	/**
	 * @return the URL where the server listens to. It has no path element.
	 */
	public URL getBaseUrl()
	{
		return httpServer.getAdvertisedAddress();
	}
	
	/**
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
	
	@Override
	public void start() throws EngineException
	{
		httpServer.deployEndpoint(this);
	}
	
	@Override
	public void destroy() throws EngineException
	{
		httpServer.undeployEndpoint(this.getEndpointDescription().getId());
	}
}
