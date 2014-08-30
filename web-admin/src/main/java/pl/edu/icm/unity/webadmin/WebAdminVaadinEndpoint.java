/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.sandbox.SandboxAuthnRouterImpl;
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

	public static final String SANDBOX_PATH = "/sandbox";

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
		
		authnFilter.addProtectedPath(SANDBOX_PATH);
		
		SandboxAuthnRouter sandboxRouter = new SandboxAuthnRouterImpl();
		
		UnityVaadinServlet sandboxServlet = new UnityVaadinServlet(applicationContext, 
				SandboxUI.class.getSimpleName(), description, null, null);
		sandboxServlet.setSandboxRouter(sandboxRouter);
		ServletHolder sandboxServletHolder = createVaadinServletHolder(sandboxServlet, true);
		sandboxServletHolder.setInitParameter("closeIdleSessions", "true");
		context.addServlet(sandboxServletHolder, SANDBOX_PATH + "/*");
		
		theServlet.setSandboxRouter(sandboxRouter);
		
		return context;
	}
}
