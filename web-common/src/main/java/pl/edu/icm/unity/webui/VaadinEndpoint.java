/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import com.vaadin.server.VaadinServlet;

import eu.unicore.util.configuration.ConfigurationException;

import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
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
	public static final String AUTHENTICATION_PATH = "/authentication";
	public static final String VAADIN_RESOURCES = "/VAADIN/*";
	public static final String SESSION_TIMEOUT_PARAM = "session-timeout";
	protected ApplicationContext applicationContext;
	protected String uiBeanName;
	protected String servletPath;
	protected Properties properties;
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
	public String getSerializedConfiguration()
	{
		CharArrayWriter writer = new CharArrayWriter();
		try
		{
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new IllegalStateException("Can not serialize endpoint's configuration", e);
		}
		return writer.toString();
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
			throw new ConfigurationException("Can't initialize the the generic IdP endpoint's configuration", e);
		}
	}

	@Override
	public synchronized ServletContextHandler getServletContextHandler()
	{
		if (context != null)
			return context;
	 	
		context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getContextAddress());
		
		AuthenticationFilter authnFilter = new AuthenticationFilter(servletPath, 
				description.getContextAddress()+AUTHENTICATION_PATH);
		context.addFilter(new FilterHolder(authnFilter), "/*", EnumSet.of(DispatcherType.REQUEST));

		EndpointRegistrationConfiguration registrationConfiguration = getRegistrationConfiguration();
		authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticators, 
				registrationConfiguration);
		ServletHolder authnServletHolder = createServletHolder(authenticationServlet);
		authnServletHolder.setInitParameter("closeIdleSessions", "true");
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticators, registrationConfiguration);
		context.addServlet(createServletHolder(theServlet), servletPath + "/*");

		return context;
	}
	
	protected ServletHolder createServletHolder(VaadinServlet servlet)
	{
		ServletHolder holder = new ServletHolder(servlet);
		holder.setInitParameter("closeIdleSessions", "true");
		int sessionTimeout = genericEndpointProperties.getIntValue(VaadinEndpointProperties.SESSION_TIMEOUT);
		holder.setInitParameter("heartbeatInterval", String.valueOf(sessionTimeout/4));
		holder.setInitParameter(SESSION_TIMEOUT_PARAM, String.valueOf(sessionTimeout));
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
