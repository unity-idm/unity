/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.provider;

import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * Implementations provide information about web applications provided by a component. 
 * We use this, instead of classic war-to-directory deployment to allow for easy testing
 * and to have unified management of all extensions, including web apps. 
 * @author K. Benedyczak
 */
public interface WebApplicationProvider
{
	
	public ServletContextHandler[] getServletContextHandlers();
}
