/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import com.vaadin.server.Constants;
import com.vaadin.server.VaadinServlet;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.HiddenResourcesFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;
import pl.edu.icm.unity.webui.authn.RememberMeProcessor;
import pl.edu.icm.unity.webui.sandbox.AccountAssociationSandboxUI;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnRouterImpl;
import pl.edu.icm.unity.webui.sandbox.TranslationProfileSandboxUI;

/**
 * Vaadin endpoint is used by all Vaadin based web endpoints. It is not a component:
 * concrete endpoint will define a custom {@link EndpointFactory} returning this class 
 * object initialized with the actual Vaadin application which should be exposed. 
 * @author K. Benedyczak
 */
public class VaadinEndpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, VaadinEndpoint.class);
	public static final String DEFAULT_THEME = "unityThemeValo";
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
	protected ProxyAuthenticationFilter proxyAuthnFilter;
	protected InvocationContextSetupFilter contextSetupFilter;
	protected UnityServerConfiguration serverConfig;
	protected UnityMessageSource msg;
	
	public VaadinEndpoint(NetworkServer server, UnityMessageSource msg, ApplicationContext applicationContext,
			String uiBeanName, String servletPath)
	{
		super(server);
		this.msg = msg;
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
				properties.setProperty(VaadinEndpointProperties.PREFIX + 
						VaadinEndpointProperties.DEF_THEME, 
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
		context.setContextPath(description.getEndpoint().getContextAddress());
		
		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		RememberMeProcessor remeberMeProcessor = applicationContext.getBean(RememberMeProcessor.class);
		
		
		context.addFilter(new FilterHolder(new HiddenResourcesFilter(
				Collections.unmodifiableList(Arrays.asList(AUTHENTICATION_PATH)))), 
				"/*", EnumSet.of(DispatcherType.REQUEST));
		authnFilter = new AuthenticationFilter(
				new ArrayList<>(Arrays.asList(uiServletPath)), 
				AUTHENTICATION_PATH, description.getRealm(), sessionMan, sessionBinder, remeberMeProcessor);
		context.addFilter(new FilterHolder(authnFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		
		proxyAuthnFilter = new ProxyAuthenticationFilter(authenticationFlows, 
				description.getEndpoint().getContextAddress(),
				genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.AUTO_LOGIN));
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
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true);
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		UnityBootstrapHandler handler4Main = getBootstrapHandler(uiServletPath);
		theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticationFlows, registrationConfiguration, properties,
				handler4Main);
		context.addServlet(createVaadinServletHolder(theServlet, false), uiServletPath + "/*");
		context.addServlet(new ServletHolder(new ForwadSerlvet()), "/");
		
		return context;
	}
	
	protected UnityBootstrapHandler getBootstrapHandler(String uiPath)
	{
		int sessionTimeout = description.getRealm().getMaxInactivity();
		return getBootstrapHandlerGeneric(uiPath, getHeartbeatInterval(sessionTimeout),
				VaadinEndpointProperties.THEME);
	}

	protected UnityBootstrapHandler getBootstrapHandler4Authn(String uiPath)
	{
		return getBootstrapHandlerGeneric(uiPath, LONG_HEARTBEAT, VaadinEndpointProperties.AUTHN_THEME);
	}

	private UnityBootstrapHandler getBootstrapHandlerGeneric(String uiPath, int heartBeat, String themeKey)
	{
		String template = genericEndpointProperties.getValue(VaadinEndpointProperties.TEMPLATE);
		boolean productionMode = genericEndpointProperties.getBooleanValue(
				VaadinEndpointProperties.PRODUCTION_MODE);
		return new UnityBootstrapHandler(getWebContentsDir(), template, msg, 
				getConfiguredTheme(themeKey), !productionMode, 
				heartBeat, uiPath);
	}
	
	private String getConfiguredTheme(String themeKey)
	{
		String theme = genericEndpointProperties.getConfiguredTheme(themeKey);
		return theme == null ? DEFAULT_THEME : theme;
	}
	
	@Override
	public final synchronized ServletContextHandler getServletContextHandler()
	{
		context = getServletContextHandlerOverridable();
		
		String webContentDir = getWebContentsDir();
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

	protected String getWebContentsDir()
	{
		if (genericEndpointProperties.isSet(VaadinEndpointProperties.WEB_CONTENT_PATH))
			return genericEndpointProperties.getValue(VaadinEndpointProperties.WEB_CONTENT_PATH);
		if (serverConfig.isSet(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH))
			return serverConfig.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		return null;
	}
	
	protected int getHeartbeatInterval(int sessionTimeout)
	{
		if (sessionTimeout >= 3*DEFAULT_HEARTBEAT) 
			return DEFAULT_HEARTBEAT;
		int ret = sessionTimeout/3;
		return ret < 2 ? 2 : ret;
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
	
	protected ServletHolder createVaadinServletHolder(VaadinServlet servlet, boolean unrestrictedSessionTime)
	{
		ServletHolder holder = createServletHolder(servlet, unrestrictedSessionTime);
		int sessionTimeout = description.getRealm().getMaxInactivity();
		int heartBeat = unrestrictedSessionTime ? LONG_HEARTBEAT : getHeartbeatInterval(sessionTimeout);
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

	private void addSandboxUI(String path, String uiBeanName, SandboxAuthnRouter sandboxRouter)
	{
		UnityBootstrapHandler bootstrapHanlder = getBootstrapHandler(path);
		UnityVaadinServlet sandboxServlet = new UnityVaadinServlet(applicationContext, 
				uiBeanName, description, authenticationFlows, null, properties, bootstrapHanlder);
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
	public final synchronized void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
	{
		setAuthenticators(authenticators);
		if (authenticationServlet != null)
		{
			authenticationServlet.updateAuthenticationFlows(authenticators);
			theServlet.updateAuthenticationFlows(authenticators);
			proxyAuthnFilter.updateAuthenticators(authenticators);
		}
	}
	
	private class ForwadSerlvet extends HttpServlet
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
