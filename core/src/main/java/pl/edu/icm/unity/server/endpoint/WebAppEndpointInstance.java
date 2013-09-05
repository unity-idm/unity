/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import java.util.List;
import java.util.Map;

import org.eclipse.jetty.servlet.ServletContextHandler;


/**
 * Servlet endpoint instance
 * @author K. Benedyczak
 */
public interface WebAppEndpointInstance extends EndpointInstance
{
	/**
	 * @return web application context
	 */
	public ServletContextHandler getServletContextHandler();
	
	/**
	 * Runtime update of the authenticators being used by this endpoint.
	 * @param handler
	 * @param authenticators
	 * @throws UnsupportedOperationException if the operation is unsupported and the endpoint must be 
	 * re-created instead.
	 */
	public void updateAuthenticators(List<Map<String, BindingAuthn>> authenticators)
		throws UnsupportedOperationException;
}
