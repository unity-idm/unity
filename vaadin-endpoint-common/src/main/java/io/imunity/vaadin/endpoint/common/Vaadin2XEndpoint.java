/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.startup.ServletContextListeners;
import eu.unicore.util.configuration.ConfigurationException;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.RememberMeProcessor;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.HiddenResourcesFilter;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import java.io.StringReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static io.imunity.vaadin.elements.VaadinInitParameters.SESSION_TIMEOUT_PARAM;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.setCurrentWebAppAuthenticationFlows;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH;

public class Vaadin2XEndpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	private static final Duration UNRESTRICTED_SESSION_TIMEOUT_VALUE = Duration.of(1, ChronoUnit.HOURS);
	public static final String AUTHENTICATION_PATH = "/authentication";
	protected ApplicationContext applicationContext;
	protected CustomResourceProvider resourceProvider;
	protected String uiServletPath;

	protected ServletContextHandler context = null;
	protected AuthenticationFilter authnFilter;
	protected ProxyAuthenticationFilter proxyAuthnFilter;
	protected UnityServerConfiguration serverConfig;
	protected MessageSource msg;

	protected InvocationContextSetupFilter contextSetupFilter;
	protected Vaadin82XEndpointProperties genericEndpointProperties;
	protected final RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter;
	protected final Class<? extends com.vaadin.flow.server.VaadinServlet> servletClass;

	public Vaadin2XEndpoint(NetworkServer server,
	                        AdvertisedAddressProvider advertisedAddrProvider,
	                        MessageSource msg,
	                        ApplicationContext applicationContext,
	                        CustomResourceProvider resourceProvider,
	                        String servletPath,
	                        RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
	                        Class<? extends com.vaadin.flow.server.VaadinServlet> servletClass)
	{
		super(server, advertisedAddrProvider);
		this.msg = msg;
		this.applicationContext = applicationContext;
		this.resourceProvider = resourceProvider;
		this.uiServletPath = servletPath;
		this.remoteAuthnResponseProcessingFilter = remoteAuthnResponseProcessingFilter;
		this.serverConfig = applicationContext.getBean(UnityServerConfiguration.class);
		this.servletClass = servletClass;
	}

	@Override
	public void setSerializedConfiguration(String cfg)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(cfg));
			genericEndpointProperties = new Vaadin82XEndpointProperties(properties, serverConfig.getValue(DEFAULT_WEB_CONTENT_PATH));

		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the generic web"
					+ " endpoint's configuration", e);
		}
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

		servletContextHandler.setContextPath(description.getEndpoint().getContextAddress());

		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		RememberMeProcessor remeberMeProcessor = applicationContext.getBean(RememberMeProcessor.class);

		servletContextHandler.addFilter(new FilterHolder(remoteAuthnResponseProcessingFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST));
		servletContextHandler.addFilter(new FilterHolder(new HiddenResourcesFilter(
				List.of(AUTHENTICATION_PATH))),
			"/*", EnumSet.of(DispatcherType.REQUEST));
		authnFilter = new AuthenticationFilter(
			description.getRealm(), sessionMan, sessionBinder, remeberMeProcessor);
		servletContextHandler.addFilter(new FilterHolder(authnFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		proxyAuthnFilter = new ProxyAuthenticationFilter(authenticationFlows,
			description.getEndpoint().getContextAddress(),
			false,
			description.getRealm());
		servletContextHandler.addFilter(new FilterHolder(proxyAuthnFilter), AUTHENTICATION_PATH + "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		contextSetupFilter = new InvocationContextSetupFilter(serverConfig, description.getRealm(),
			getServletUrl(uiServletPath), getAuthenticationFlows());
		servletContextHandler.addFilter(new FilterHolder(contextSetupFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		return servletContextHandler;
	}

	protected ServletHolder createServletHolder(Servlet servlet)
	{
		ServletHolder holder = new ServletHolder(servlet);
		holder.setInitParameter("closeIdleSessions", "true");
		holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(UNRESTRICTED_SESSION_TIMEOUT_VALUE.getSeconds()));

		return holder;
	}

	protected String getWebContentsDir()
	{
		if (genericEndpointProperties.isSet(VaadinEndpointProperties.WEB_CONTENT_PATH))
			return genericEndpointProperties.getValue(VaadinEndpointProperties.WEB_CONTENT_PATH);
		if (serverConfig.isSet(DEFAULT_WEB_CONTENT_PATH))
			return serverConfig.getValue(DEFAULT_WEB_CONTENT_PATH);
		return null;
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
		Vaadin2XWebAppContext vaadin2XWebAppContext = new Vaadin2XWebAppContext(properties, genericEndpointProperties, msg, description, authenticationFlows, null);
		context = getServletContextHandlerOverridable(vaadin2XWebAppContext);
		return context;
	}

	protected WebAppContext getWebAppContext(WebAppContext context, String contextPath, Set<String> classPathElements, String webResourceRootUri,
	                               EventListener eventListener) {
		context.setResourceBase(webResourceRootUri);
		context.setContextPath(contextPath);
		context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", JarGetter.getJarsRegex(classPathElements));
		context.setConfigurationDiscovered(true);
		context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		ServletHolder servletHolder = context.addServlet(servletClass, "/*");
		servletHolder.setAsyncSupported(true);
		servletHolder.setInitParameter(InitParameters.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS, "true");
		context.getServletContext().setExtendedListenerTypes(true);
		if(eventListener != null)
			context.addEventListener(eventListener);

		return context;
	}

	@Override
	public synchronized void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
	{
		setAuthenticators(authenticators);
		setCurrentWebAppAuthenticationFlows(authenticators);
		proxyAuthnFilter.updateAuthenticators(authenticators);
	}
}
