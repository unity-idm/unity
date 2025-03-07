/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.server;

import static io.imunity.vaadin.elements.VaadinInitParameters.SESSION_TIMEOUT_PARAM;

import java.util.EnumSet;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;

import io.imunity.vaadin.endpoint.common.CustomResourceProvider;
import io.imunity.vaadin.endpoint.common.InvocationContextSetupFilter;
import io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter;
import io.imunity.vaadin.endpoint.common.Vaadin2XEndpoint;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.RememberMeProcessor;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;

public class SecureVaadin2XEndpoint extends Vaadin2XEndpoint
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, SecureVaadin2XEndpoint.class);
	public static final String AUTHENTICATION_PATH = "/authentication";
	protected AuthenticationFilter authnFilter;
	protected ProxyAuthenticationFilter proxyAuthnFilter;

	public SecureVaadin2XEndpoint(NetworkServer server,
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

	@Override
	protected ServletContextHandler getServletContextHandlerOverridable(WebAppContext webAppContext)
	{
		if (context != null)
			return context;

		ServletContextHandler servletContextHandler;
		try
		{
			servletContextHandler = getWebAppContext(webAppContext);
		} catch (Exception e)
		{
			log.error("Exception occurred, while web app context creating", e);
			return context;
		}

		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		RememberMeProcessor remeberMeProcessor = applicationContext.getBean(RememberMeProcessor.class);

		servletContextHandler.addFilter(new FilterHolder(remoteAuthnResponseProcessingFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST));
		authnFilter = new AuthenticationFilter(
			description.getRealm(), sessionMan, sessionBinder, remeberMeProcessor);
		servletContextHandler.addFilter(new FilterHolder(authnFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		proxyAuthnFilter = new ProxyAuthenticationFilter(authenticationFlows,
			description.getEndpoint().getContextAddress(),
			genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.AUTO_LOGIN),
			description.getRealm());
		servletContextHandler.addFilter(new FilterHolder(proxyAuthnFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		contextSetupFilter = new InvocationContextSetupFilter(serverConfig, description.getRealm(),
			getServletUrl(uiServletPath), getAuthenticationFlows());
		servletContextHandler.addFilter(new FilterHolder(contextSetupFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		return servletContextHandler;
	}

	protected final ServletHolder createServletHolder(Servlet servlet)
	{
		ServletHolder holder = new ServletHolder(servlet);
		holder.setInitParameter("closeIdleSessions", "true");
		holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(UNRESTRICTED_SESSION_TIMEOUT_VALUE.getSeconds()));

		return holder;
	}

	@Override
	public synchronized void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
	{
		super.updateAuthenticationFlows(authenticators);
		proxyAuthnFilter.updateAuthenticators(authenticators);
	}
}
