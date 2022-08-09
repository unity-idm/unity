/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common;

import com.vaadin.flow.server.startup.ServletContextListeners;
import com.vaadin.server.Constants;
import com.vaadin.server.VaadinServlet;
import eu.unicore.util.configuration.ConfigurationException;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.*;
import org.springframework.context.ApplicationContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
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
import pl.edu.icm.unity.webui.*;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.*;

import static pl.edu.icm.unity.webui.VaadinEndpoint.*;

public class Vaadin823Endpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, Vaadin823Endpoint.class);
	public static final String AUTHENTICATION_PATH = "/authentication";
	protected ApplicationContext applicationContext;
	protected CustomResourceProvider resourceProvider;
	protected String uiServletPath;

	protected ServletContextHandler context = null;
	protected UnityVaadinServlet authenticationServlet;
	protected AuthenticationFilter authnFilter;
	protected ProxyAuthenticationFilter proxyAuthnFilter;
	protected UnityServerConfiguration serverConfig;
	protected MessageSource msg;

	protected InvocationContextSetupFilter contextSetupFilter;
	protected VaadinEndpointProperties genericEndpointProperties;

	protected final RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter;
	
	public Vaadin823Endpoint(NetworkServer server,
	                         AdvertisedAddressProvider advertisedAddrProvider,
	                         MessageSource msg,
	                         ApplicationContext applicationContext,
	                         CustomResourceProvider resourceProvider,
	                         String servletPath,
	                         RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter)
	{
		super(server, advertisedAddrProvider);
		this.msg = msg;
		this.applicationContext = applicationContext;
		this.resourceProvider = resourceProvider;
		this.uiServletPath = servletPath;
		this.remoteAuthnResponseProcessingFilter = remoteAuthnResponseProcessingFilter;
		serverConfig = applicationContext.getBean(UnityServerConfiguration.class);
	}

	@Override
	public void setSerializedConfiguration(String cfg)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(cfg));
			genericEndpointProperties = new VaadinEndpointProperties(properties);

		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the generic web"
					+ " endpoint's configuration", e);
		}
	}

	protected ServletContextHandler getServletContextHandlerOverridable()
	{
		if (context != null)
			return context;

		ServletContextHandler context;
		try
		{
			context = getWebAppContext(uiServletPath,
					resourceProvider.getChosenClassPathElement(),
					resourceProvider.getClientResource("META-INF/resources/").toURI(),
					new ServletContextListeners()
			);
		} catch (Exception e)
		{
			return this.context;
		}

		context.setContextPath(description.getEndpoint().getContextAddress());

		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		RememberMeProcessor remeberMeProcessor = applicationContext.getBean(RememberMeProcessor.class);

		context.addFilter(new FilterHolder(remoteAuthnResponseProcessingFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST));
		context.addFilter(new FilterHolder(new HiddenResourcesFilter(
				List.of(AUTHENTICATION_PATH))),
			"/*", EnumSet.of(DispatcherType.REQUEST));
		authnFilter = new AuthenticationFilter(
			new ArrayList<>(List.of(uiServletPath)),
			AUTHENTICATION_PATH, description.getRealm(), sessionMan, sessionBinder, remeberMeProcessor);
		context.addFilter(new FilterHolder(authnFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		proxyAuthnFilter = new ProxyAuthenticationFilter(authenticationFlows,
			description.getEndpoint().getContextAddress(),
			false,
			description.getRealm());
		context.addFilter(new FilterHolder(proxyAuthnFilter), AUTHENTICATION_PATH + "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		contextSetupFilter = new InvocationContextSetupFilter(serverConfig, description.getRealm(),
			getServletUrl(uiServletPath), getAuthenticationFlows());
		context.addFilter(new FilterHolder(contextSetupFilter), "/*",
			EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		EndpointRegistrationConfiguration registrationConfiguration = genericEndpointProperties.getRegistrationConfiguration();

		UnityBootstrapHandler handler4Authn = getBootstrapHandler4Authn(uiServletPath);
		authenticationServlet = new AuthenticationVaadinServlet(applicationContext,
			description, authenticationFlows,
			registrationConfiguration, properties, handler4Authn);
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet);
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH + "/*");
		context.addServlet(authnServletHolder, "/VAADIN/vaadinBootstrap.js*");
		context.addServlet(authnServletHolder, "/VAADIN/widgetsets/*");
		context.addServlet(authnServletHolder, "/VAADIN/themes/*");

		context.addServlet(new ServletHolder(new ForwardServlet()), "/*");

		return context;
	}

	protected UnityBootstrapHandler getBootstrapHandler(String uiPath)
	{
		int sessionTimeout = description.getRealm().getMaxInactivity();
		return getBootstrapHandlerGeneric(uiPath, getHeartbeatInterval(sessionTimeout),
			genericEndpointProperties.getEffectiveMainTheme());
	}

	protected int getHeartbeatInterval(int sessionTimeout)
	{
		if (sessionTimeout >= 3*DEFAULT_HEARTBEAT)
			return DEFAULT_HEARTBEAT;
		int ret = sessionTimeout/3;
		return ret < 2 ? 2 : ret;
	}

	protected ServletHolder createVaadinServletHolder(VaadinServlet servlet)
	{
		ServletHolder holder = createServletHolder(servlet, true);

		int heartBeat = LONG_HEARTBEAT;
		log.debug("Servlet " + servlet.toString() + " - heartBeat=" +heartBeat);

		boolean productionMode = genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.PRODUCTION_MODE);
		holder.setInitParameter("heartbeatInterval", String.valueOf(heartBeat));
		holder.setInitParameter("sendUrlsAsParameters", "false"); //theoreticly needed for push state navi, but adding this causes NPEs
		holder.setInitParameter(PRODUCTION_MODE_PARAM, String.valueOf(productionMode));
		holder.setInitParameter("org.atmosphere.cpr.broadcasterCacheClass",
			"org.atmosphere.cache.UUIDBroadcasterCache");
		holder.setInitParameter(Constants.PARAMETER_WIDGETSET,
			"pl.edu.icm.unity.webui.customWidgetset");
		return holder;
	}

	protected ServletHolder createServletHolder(Servlet servlet, boolean unrestrictedSessionTime)
	{
		ServletHolder holder = new ServletHolder(servlet);
		holder.setInitParameter("closeIdleSessions", "true");

		if (unrestrictedSessionTime)
		{
			holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(LONG_SESSION));
		} else
		{
			int sessionTimeout = description.getRealm().getMaxInactivity();
			int heartBeat = getHeartbeatInterval(sessionTimeout);
			sessionTimeout = sessionTimeout - heartBeat;
			if (sessionTimeout < 2)
				sessionTimeout = 2;
			holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(sessionTimeout));
		}
		return holder;
	}

	protected UnityBootstrapHandler getBootstrapHandler4Authn(String uiPath)
	{
		return getBootstrapHandlerGeneric(uiPath, LONG_HEARTBEAT, genericEndpointProperties.getEffectiveAuthenticationTheme());
	}

	protected UnityBootstrapHandler getBootstrapHandlerGeneric(String uiPath, int heartBeat, String theme)
	{
		String template = genericEndpointProperties.getValue(VaadinEndpointProperties.TEMPLATE);
		boolean productionMode = genericEndpointProperties.getBooleanValue(
			VaadinEndpointProperties.PRODUCTION_MODE);
		return new UnityBootstrapHandler(getWebContentsDir(), template, msg,
			theme, !productionMode,
			heartBeat, uiPath);
	}

	protected String getWebContentsDir()
	{
		if (genericEndpointProperties.isSet(VaadinEndpointProperties.WEB_CONTENT_PATH))
			return genericEndpointProperties.getValue(VaadinEndpointProperties.WEB_CONTENT_PATH);
		if (serverConfig.isSet(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH))
			return serverConfig.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		return null;
	}

	@Override
	public synchronized ServletContextHandler getServletContextHandler()
	{
		context = getServletContextHandlerOverridable();
		return context;
	}

	WebAppContext getWebAppContext(String contextPath, Set<String> classPathElements, URI webResourceRootUri,
	                               EventListener eventListener) throws Exception {
		WebAppContext context = new Vaadin23WebAppContext(properties, genericEndpointProperties);
		context.setBaseResource(Resource.newResource(webResourceRootUri));
		context.setContextPath(contextPath);
		context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", JarGetter.getJarsRegex(classPathElements));
		context.setConfigurationDiscovered(true);

		context.setConfigurations(new Configuration[]{
			new AnnotationConfiguration(),
			new WebInfConfiguration(),
			new WebXmlConfiguration(),
			new MetaInfConfiguration(),
			new FragmentConfiguration(),
			new EnvConfiguration(),
			new PlusConfiguration(),
			new JettyWebXmlConfiguration()
		});
		context.getServletContext().setExtendedListenerTypes(true);
		if(eventListener != null)
			context.addEventListener(eventListener);

		return context;
	}

	@Override
	public synchronized void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
	{
		setAuthenticators(authenticators);
		if (authenticationServlet != null)
		{
			authenticationServlet.updateAuthenticationFlows(authenticators);
			proxyAuthnFilter.updateAuthenticators(authenticators);
		}
	}

	class ForwardServlet extends HttpServlet
	{
		@Override
		protected void service(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
		{
			ServletContext servletContext = req.getServletContext();
			String uriWithoutContext = req.getPathInfo();
			if (uriWithoutContext == null)
				uriWithoutContext = "";
			String targetPath = uiServletPath + uriWithoutContext;
			log.trace("Forward from " + req.getRequestURI() + " -> " +
				req.getContextPath() + targetPath);
			servletContext.getRequestDispatcher(targetPath).forward(req, res);
		}
	}
}
