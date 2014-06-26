/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.ByteArrayInputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;

import org.apache.log4j.Logger;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import com.vaadin.server.VaadinServlet;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;

/**
 * Vaadin endpoint is used by all Vaadin based web endpoints. It is not a component:
 * concrete endpoint will define a custom {@link EndpointFactory} returning this class 
 * object initialized with the actual Vaadin application which should be exposed. 
 * @author K. Benedyczak
 */
public class VaadinEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, VaadinEndpoint.class);
	public static final int DEFAULT_HEARTBEAT = 10;
	public static final String AUTHENTICATION_PATH = "/authentication";
	public static final String VAADIN_RESOURCES = "/VAADIN/*";
	public static final String SESSION_TIMEOUT_PARAM = "session-timeout";
	public static final String PRODUCTION_MODE_PARAM = "productionMode";
	protected ApplicationContext applicationContext;
	protected String uiBeanName;
	protected String servletPath;
	protected VaadinEndpointProperties genericEndpointProperties;

	protected ServletContextHandler context = null;
	protected UnityVaadinServlet theServlet;
	protected UnityVaadinServlet authenticationServlet;
	
	public VaadinEndpoint(EndpointTypeDescription type, ApplicationContext applicationContext,
			String uiBeanName, String servletPath)
	{
		super(type);
		this.applicationContext = applicationContext;
		this.uiBeanName = uiBeanName;
		this.servletPath = servletPath;
	}

	@Override
	public void setSerializedConfiguration(String cfg)
	{
		properties = new Properties();
		try
		{
			properties.load(new ByteArrayInputStream(cfg.getBytes()));
			genericEndpointProperties = new VaadinEndpointProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the generic web"
					+ " endpoint's configuration", e);
		}
	}

	@Override
	public synchronized ServletContextHandler getServletContextHandler()
	{
		if (context != null)
			return context;
	 	
		context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getContextAddress());
		
		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		
		AuthenticationFilter authnFilter = new AuthenticationFilter(servletPath, 
				AUTHENTICATION_PATH, description.getRealm(), sessionMan, sessionBinder);
		context.addFilter(new FilterHolder(authnFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST));

		EndpointRegistrationConfiguration registrationConfiguration = getRegistrationConfiguration();

		authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticators, 
				registrationConfiguration);
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true);
		authnServletHolder.setInitParameter("closeIdleSessions", "true");
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticators, registrationConfiguration);
		context.addServlet(createVaadinServletHolder(theServlet, false), servletPath + "/*");

		return context;
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
			holder.setInitParameter("closeIdleSessions", "false");
			holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(-1));
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
		int heartBeat = getHeartbeatInterval(sessionTimeout);
		log.debug("Servlet " + servlet.toString() + " - heartBeat=" +heartBeat);
			
		boolean productionMode = genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.PRODUCTION_MODE);
		holder.setInitParameter("heartbeatInterval", String.valueOf(heartBeat));
		holder.setInitParameter(PRODUCTION_MODE_PARAM, String.valueOf(productionMode));
		holder.setInitParameter("org.atmosphere.cpr.broadcasterCacheClass", 
				"org.atmosphere.cache.UUIDBroadcasterCache");
		return holder;
	}

	protected EndpointRegistrationConfiguration getRegistrationConfiguration()
	{
		return new EndpointRegistrationConfiguration(genericEndpointProperties.getListOfValues(
				VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS),
				genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.ENABLE_REGISTRATION));
	}
	
	@Override
	public synchronized void updateAuthenticators(List<Map<String, BindingAuthn>> authenticators)
	{
		setAuthenticators(authenticators);
		if (authenticationServlet != null)
		{
			authenticationServlet.updateAuthenticators(authenticators);
			theServlet.updateAuthenticators(authenticators);
		}
	}
}
