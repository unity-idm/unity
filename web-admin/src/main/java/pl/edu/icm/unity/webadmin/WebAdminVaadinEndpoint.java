/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.sandbox.SandboxUI;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;

/**
 * Extends a simple {@link VaadinEndpoint} with sandbox servlet.
 * 
 * @author Roman Krysinski
 */
public class WebAdminVaadinEndpoint extends VaadinEndpoint 
{

	private static final String SANDBOX_PATH = "/sandbox";

	public WebAdminVaadinEndpoint(EndpointTypeDescription type,
			ApplicationContext applicationContext, String uiBeanName,
			String servletPath) 
	{
		super(type, applicationContext, uiBeanName, servletPath);
	}

	@Override
	public synchronized ServletContextHandler getServletContextHandler() 
	{
		if (context != null)
		{
			return context;
		}
		
		context = super.getServletContextHandler();
		
		UnityVaadinServlet sandboxServlet = new UnityVaadinServlet(applicationContext, 
				SandboxUI.class.getSimpleName(), description, null, null);
		ServletHolder sandboxServletHolder = createVaadinServletHolder(sandboxServlet, true);
		sandboxServletHolder.setInitParameter("closeIdleSessions", "true");
		context.addServlet(sandboxServletHolder, SANDBOX_PATH + "/*");
		
		return context;
	}
}
