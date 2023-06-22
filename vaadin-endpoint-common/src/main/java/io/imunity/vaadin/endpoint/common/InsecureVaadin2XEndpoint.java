/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.server.startup.ServletContextListeners;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

import static io.imunity.vaadin.elements.VaadinInitParameters.SESSION_TIMEOUT_PARAM;

public class InsecureVaadin2XEndpoint extends Vaadin2XEndpoint
{
	public InsecureVaadin2XEndpoint(NetworkServer server,
	                                AdvertisedAddressProvider advertisedAddrProvider,
	                                MessageSource msg,
	                                ApplicationContext applicationContext,
	                                CustomResourceProvider resourceProvider,
	                                String servletPath,
	                                RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
	                                SandboxAuthnRouter sandboxAuthnRouter,
	                                Class<? extends com.vaadin.flow.server.VaadinServlet> servletClass)
	{
		super(server, advertisedAddrProvider, msg, applicationContext, resourceProvider, servletPath,
				remoteAuthnResponseProcessingFilter, sandboxAuthnRouter, servletClass);
	}

	protected ServletContextHandler getServletContextHandlerOverridable(WebAppContext webAppContext)
	{
		if (context != null)
			return context;

		ServletContextHandler servletContextHandler;
		try
		{
			servletContextHandler = getWebAppContext(webAppContext, uiServletPath,
					resourceProvider.getChosenClassPathElement(),
					getWebContentsDir(),
					new ServletContextListeners()
			);
		} catch (Exception e)
		{
			return context;
		}

		servletContextHandler.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(UNRESTRICTED_SESSION_TIMEOUT_VALUE.getSeconds()));
		servletContextHandler.setContextPath(description.getEndpoint().getContextAddress());

		servletContextHandler.addFilter(new FilterHolder(remoteAuthnResponseProcessingFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST));

		contextSetupFilter = new InvocationContextSetupFilter(serverConfig, description.getRealm(),
			getServletUrl(uiServletPath), getAuthenticationFlows());
		servletContextHandler.addFilter(new FilterHolder(contextSetupFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		return servletContextHandler;
	}
}
