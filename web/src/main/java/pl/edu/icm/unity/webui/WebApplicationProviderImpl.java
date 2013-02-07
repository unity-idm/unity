/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import pl.edu.icm.unity.server.provider.WebApplicationProvider;

/**
 * Provides info about the servlet to the loader module.
 * @author K. Benedyczak
 */
public class WebApplicationProviderImpl implements WebApplicationProvider
{
	@Override
	public ServletContextHandler[] getServletContextHandlers()
	{
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/webui");
		context.addServlet(new ServletHolder(new WebUIServlet()),"/*");
		return new ServletContextHandler[] {context};
	}
}
