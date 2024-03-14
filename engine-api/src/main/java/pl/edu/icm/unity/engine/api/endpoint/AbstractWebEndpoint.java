/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.endpoint;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;

import java.net.URL;

/**
 * Typical boilerplate for all {@link WebAppEndpointInstance}s.
 * @author K. Benedyczak
 */
public abstract class AbstractWebEndpoint extends AbstractEndpoint
{
	protected final NetworkServer httpServer;
	protected final AdvertisedAddressProvider advertisedAddrProvider;
	
	public AbstractWebEndpoint(NetworkServer httpServer, AdvertisedAddressProvider advertisedAddrProvider)
	{
		this.httpServer = httpServer;
		this.advertisedAddrProvider = advertisedAddrProvider;
	}

	/**
	 * @return the URL where the server listens to. It has no path element.
	 */
	public URL getBaseUrl()
	{
		return advertisedAddrProvider.get();
	}
	
	/**
	 * @param servletPath path of the servlet exposing the endpoint, Only the servlet's path, without context prefix.
	 * @return URL in string form, including the servers address, context address and 
	 * the servlet's address. 
	 */
	public String getServletUrl(String servletPath)
	{
		return getBaseUrl().toExternalForm() +
				getEndpointDescription().getEndpoint().getContextAddress() + 
				servletPath;
	}
	
	@Override
	public final void start() throws EngineException
	{
		startOverridable();
		if(this instanceof WebAppEndpointInstance ee10)
			httpServer.deployEndpoint(ee10);
		else
			throw new IllegalStateException("Endpoint have to implement one of WebAppEndpointEEInstance");
	}
	
	protected void startOverridable()
	{
	}
	
	@Override
	public final void destroy() throws EngineException
	{
		super.destroy();
		httpServer.undeployEndpoint(this.getEndpointDescription().getEndpoint().getName());
		destroyOverridable();
	}
	
	protected void destroyOverridable()
	{
	}
}
