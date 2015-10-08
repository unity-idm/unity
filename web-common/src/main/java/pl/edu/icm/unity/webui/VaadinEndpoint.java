/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;

import org.apache.log4j.Logger;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.sandbox.AccountAssociationSandboxUI;
import pl.edu.icm.unity.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.sandbox.SandboxAuthnRouterImpl;
import pl.edu.icm.unity.sandbox.TranslationProfileSandboxUI;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.utils.HiddenResourcesFilter;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;

import com.vaadin.server.Constants;
import com.vaadin.server.VaadinServlet;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * Vaadin endpoint is used by all Vaadin based web endpoints. It is not a component:
 * concrete endpoint will define a custom {@link EndpointFactory} returning this class 
 * object initialized with the actual Vaadin application which should be exposed. 
 * @author K. Benedyczak
 */
public class VaadinEndpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, VaadinEndpoint.class);
	public static final int DEFAULT_HEARTBEAT = 10;
	public static final int LONG_SESSION = 3600;
	public static final int LONG_HEARTBEAT = 300;
	public static final String AUTHENTICATION_PATH = "/authentication";
	public static final String SANDBOX_PATH_TRANSLATION = "/sandbox-translation";
	public static final String SANDBOX_PATH_ASSOCIATION = "/sandbox-association";
	public static final String VAADIN_RESOURCES = "/VAADIN/*";
	public static final String SESSION_TIMEOUT_PARAM = "session-timeout";
	public static final String PRODUCTION_MODE_PARAM = "productionMode";
	protected ApplicationContext applicationContext;
	protected String uiBeanName;
	protected String uiServletPath;
	protected VaadinEndpointProperties genericEndpointProperties;

	protected ServletContextHandler context = null;
	protected UnityVaadinServlet theServlet;
	protected UnityVaadinServlet authenticationServlet;
	protected AuthenticationFilter authnFilter;
	protected InvocationContextSetupFilter contextSetupFilter;
	protected UnityServerConfiguration serverConfig;
	
	public VaadinEndpoint(NetworkServer server, ApplicationContext applicationContext,
			String uiBeanName, String servletPath)
	{
		super(server);
		this.applicationContext = applicationContext;
		this.uiBeanName = uiBeanName;
		this.uiServletPath = servletPath;
		serverConfig = applicationContext.getBean(UnityServerConfiguration.class);		

	}

	@Override
	public void setSerializedConfiguration(String cfg)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(cfg));
			//a copy is set to endpoint's configuration so that the default is easily accessible
			if (serverConfig.isSet(UnityServerConfiguration.THEME))
				properties.setProperty(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.DEF_THEME, 
					serverConfig.getValue(UnityServerConfiguration.THEME));
			
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
	 	
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getContextAddress());
		
		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		
		context.addFilter(new FilterHolder(new HiddenResourcesFilter(
				Collections.unmodifiableList(Arrays.asList(AUTHENTICATION_PATH)))), 
				"/*", EnumSet.of(DispatcherType.REQUEST));
		authnFilter = new AuthenticationFilter(
				new ArrayList<String>(Arrays.asList(uiServletPath)), 
				AUTHENTICATION_PATH, description.getRealm(), sessionMan, sessionBinder);
		context.addFilter(new FilterHolder(authnFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		contextSetupFilter = new InvocationContextSetupFilter(serverConfig, description.getRealm(),
				getServletUrl(uiServletPath));
		context.addFilter(new FilterHolder(contextSetupFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		EndpointRegistrationConfiguration registrationConfiguration = getRegistrationConfiguration();
		
		authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticators, 
				registrationConfiguration, properties);
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true);
		authnServletHolder.setInitParameter("closeIdleSessions", "true");
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticators, registrationConfiguration, properties);
		context.addServlet(createVaadinServletHolder(theServlet, false), uiServletPath + "/*");
		
		return context;
	}
	
	@Override
	public final synchronized ServletContextHandler getServletContextHandler()
	{
		context = getServletContextHandlerOverridable();
		
		String webContentDir = getWebContentsDir(serverConfig);
		if (webContentDir != null)
			context.setResourceBase(webContentDir);
		
		SandboxAuthnRouter sandboxRouter = new SandboxAuthnRouterImpl();

		addSandboxUI(SANDBOX_PATH_ASSOCIATION, AccountAssociationSandboxUI.class.getSimpleName(), 
				sandboxRouter);
		addProtectedSandboxUI(SANDBOX_PATH_TRANSLATION, TranslationProfileSandboxUI.class.getSimpleName(), 
				sandboxRouter);
		theServlet.setSandboxRouter(sandboxRouter);
		authenticationServlet.setSandboxRouter(sandboxRouter);
		
		return context;
	}

	protected String getWebContentsDir(UnityServerConfiguration config)
	{
		if (genericEndpointProperties.isSet(VaadinEndpointProperties.WEB_CONTENT_PATH))
			return genericEndpointProperties.getValue(VaadinEndpointProperties.WEB_CONTENT_PATH);
		if (config.isSet(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH))
			return config.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		return null;
	}
	
	protected int getHeartbeatInterval(int sessionTimeout)
	{
		if (sessionTimeout >= 2*DEFAULT_HEARTBEAT) 
			return DEFAULT_HEARTBEAT;
		return sessionTimeout/2;
	}
	
	/**
	 * Sets HTTP session timeout. If set, then it is set to less then the realm's login session timeout.
	 * the delta is computed in such way that vaadin is able to send at least one heart beat after the 
	 * HTTP/vaadin session expires, but before the login session expires.
	 * @param servlet
	 * @param unrestrictedSessionTime
	 * @return
	 */
	protected ServletHolder createServletHolder(Servlet servlet, boolean unrestrictedSessionTime)
	{
		ServletHolder holder = new ServletHolder(servlet);

		if (unrestrictedSessionTime)
		{
			holder.setInitParameter("closeIdleSessions", "true");
			holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(LONG_SESSION));
		} else
		{
			holder.setInitParameter("closeIdleSessions", "true");
			int sessionTimeout = description.getRealm().getMaxInactivity();
			int heartBeat = getHeartbeatInterval(sessionTimeout);
			sessionTimeout = sessionTimeout - heartBeat - heartBeat/2;
			if (sessionTimeout < 2)
				sessionTimeout = 2;
			holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(sessionTimeout));
		}
		return holder;
	}
	
	protected ServletHolder createVaadinServletHolder(VaadinServlet servlet, boolean unrestrictedSessionTime)
	{
		ServletHolder holder = createServletHolder(servlet, unrestrictedSessionTime);
		int sessionTimeout = description.getRealm().getMaxInactivity();
		int heartBeat = unrestrictedSessionTime ? LONG_HEARTBEAT : getHeartbeatInterval(sessionTimeout);
		log.debug("Servlet " + servlet.toString() + " - heartBeat=" +heartBeat);
			
		boolean productionMode = genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.PRODUCTION_MODE);
		holder.setInitParameter("heartbeatInterval", String.valueOf(heartBeat));
		holder.setInitParameter(PRODUCTION_MODE_PARAM, String.valueOf(productionMode));
		holder.setInitParameter("org.atmosphere.cpr.broadcasterCacheClass", 
				"org.atmosphere.cache.UUIDBroadcasterCache");
		holder.setInitParameter(Constants.PARAMETER_WIDGETSET, 
				"pl.edu.icm.unity.webui.customWidgetset");
		return holder;
	}

	protected EndpointRegistrationConfiguration getRegistrationConfiguration()
	{
		return new EndpointRegistrationConfiguration(genericEndpointProperties.getListOfValues(
				VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS),
				genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.ENABLE_REGISTRATION));
	}

	private void addSandboxUI(String path, String uiBeanName, SandboxAuthnRouter sandboxRouter)
	{
		UnityVaadinServlet sandboxServlet = new UnityVaadinServlet(applicationContext, 
				uiBeanName, description, authenticators, null, properties);
		sandboxServlet.setSandboxRouter(sandboxRouter);
		ServletHolder sandboxServletHolder = createVaadinServletHolder(sandboxServlet, true);
		sandboxServletHolder.setInitParameter("closeIdleSessions", "true");
		context.addServlet(sandboxServletHolder, path + "/*");
	}
	
	private void addProtectedSandboxUI(String path, String uiBeanName, SandboxAuthnRouter sandboxRouter)
	{
		authnFilter.addProtectedPath(path);
		addSandboxUI(path, uiBeanName, sandboxRouter);
	}
	
	@Override
	public synchronized void updateAuthenticationOptions(List<AuthenticationOption> authenticators)
	{
		setAuthenticators(authenticators);
		if (authenticationServlet != null)
		{
			authenticationServlet.updateAuthenticators(authenticators);
			theServlet.updateAuthenticators(authenticators);
		}
	}
}
