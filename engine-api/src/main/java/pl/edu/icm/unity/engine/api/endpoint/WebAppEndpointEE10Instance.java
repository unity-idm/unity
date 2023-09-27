/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.endpoint;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;

/**
 * Servlet endpoint instance
 */
public interface WebAppEndpointEE10Instance extends EndpointInstance
{
	ServletContextHandler getServletContextHandler();
}
