/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

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
}
