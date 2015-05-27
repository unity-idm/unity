/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;

/**
 * Extends a simple {@link VaadinEndpoint} with sandbox servlet.
 * 
 * @author Roman Krysinski
 */
public class VaadinEndpointWithSandbox extends VaadinEndpoint 
{

	public static final String SANDBOX_PATH_TRANSLATION = "/sandbox";
	public static final String SANDBOX_PATH_ASSOCIATION = "/sandbox-association";

	public VaadinEndpointWithSandbox(EndpointTypeDescription type,
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
		SandboxAuthnRouter sandboxRouter = new SandboxAuthnRouterImpl();

		addSandboxUI(SANDBOX_PATH_ASSOCIATION, AccountAssociationSandboxUI.class.getSimpleName(), 
				sandboxRouter);
		addSandboxUI(SANDBOX_PATH_TRANSLATION, TranslationProfileSandboxUI.class.getSimpleName(), 
				sandboxRouter);
		theServlet.setSandboxRouter(sandboxRouter);
		return context;
	}
	
	private void addSandboxUI(String path, String uiBeanName, SandboxAuthnRouter sandboxRouter)
	{
		authnFilter.addProtectedPath(path);
		
		UnityVaadinServlet sandboxServlet = new UnityVaadinServlet(applicationContext, 
				uiBeanName, description, authenticators, null, properties);
		sandboxServlet.setSandboxRouter(sandboxRouter);
		ServletHolder sandboxServletHolder = createVaadinServletHolder(sandboxServlet, true);
		sandboxServletHolder.setInitParameter("closeIdleSessions", "true");
		context.addServlet(sandboxServletHolder, path + "/*");
	}
}
