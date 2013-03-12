/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Provides info about the servlet to the loader module.
 * TODO remove - temp code.
 * @author K. Benedyczak
 */
public class WebApplicationProviderImpl
{
	public ServletContextHandler[] getServletContextHandlers()
	{
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/webui");
		context.addServlet(new ServletHolder(new WebUIServlet()),"/*");
		return new ServletContextHandler[] {context};
	}
}
