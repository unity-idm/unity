/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.endpoint;

import org.eclipse.jetty.ee8.servlet.ServletContextHandler;

/**
 * Servlet endpoint instance
 */
public interface WebAppEndpointEE8Instance extends EndpointInstance
{
	ServletContextHandler getServletContextHandler();
}