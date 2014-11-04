/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * OAuth2 authorization endpoint, Vaadin based.
 * @author K. Benedyczak
 */
public class OAuthAuthzWebEndpoint extends VaadinEndpoint
{
	private OAuthASProperties oauthProperties;
	private String consumerServletPath;
	private FreemarkerHandler freemarkerHandler;
	private IdentitiesManagement identitiesManagement;
	private AttributesManagement attributesManagement;
	private PKIManagement pkiManagement;
	private OAuthEndpointsCoordinator coordinator;
	
	public OAuthAuthzWebEndpoint(EndpointTypeDescription type,
			ApplicationContext applicationContext, String uiServletPath,
			String consumerServletPath, FreemarkerHandler freemarkerHandler,
			IdentitiesManagement identitiesManagement, AttributesManagement attributesManagement,
			PKIManagement pkiManagement, OAuthEndpointsCoordinator coordinator)
	{
		super(type, applicationContext, OAuthAuthzUI.class.getSimpleName(), uiServletPath);
		this.consumerServletPath = consumerServletPath;
		this.freemarkerHandler = freemarkerHandler;
		this.attributesManagement = attributesManagement;
		this.identitiesManagement = identitiesManagement;
		this.pkiManagement = pkiManagement;
		this.coordinator = coordinator;
	}

	@Override
	public void setSerializedConfiguration(String properties)
	{
		super.setSerializedConfiguration(properties);
		try
		{
			oauthProperties = new OAuthASProperties(this.properties, pkiManagement, 
					getServletUrl(OAuthAuthzWebEndpointFactory.OAUTH_CONSUMER_SERVLET_PATH));
			coordinator.registerTokenEndpoint(oauthProperties.getValue(OAuthASProperties.ISSUER_URI), 
					getServletUrl(consumerServletPath));
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the OAuth 2 AS endpoint's configuration", e);
		}
	}
	
	@Override
	public ServletContextHandler getServletContextHandler()
	{
	 	ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getContextAddress());
		
		String endpointURL = getServletUrl(consumerServletPath);
		String uiURL = getServletUrl(servletPath);
		Filter oauthGuardFilter = new OAuthGuardFilter(servletPath, new ErrorHandler(freemarkerHandler));
		context.addFilter(new FilterHolder(oauthGuardFilter), servletPath + "/*", 
				EnumSet.of(DispatcherType.REQUEST));
		
		Servlet samlParseServlet = new OAuthParseServlet(oauthProperties, endpointURL, 
				uiURL, new ErrorHandler(freemarkerHandler), identitiesManagement, 
				attributesManagement); 
		ServletHolder samlParseHolder = createServletHolder(samlParseServlet, true);
		context.addServlet(samlParseHolder, consumerServletPath + "/*");
		
		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		
		AuthenticationFilter authnFilter = new AuthenticationFilter(servletPath, 
				AUTHENTICATION_PATH, description.getRealm(), sessionMan, sessionBinder);
		context.addFilter(new FilterHolder(authnFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST));
		
		EndpointRegistrationConfiguration registrationConfiguration = getRegistrationConfiguration();
		UnityVaadinServlet authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticators,
				registrationConfiguration);
		
		authenticationServlet.setCancelHandler(new OAuthCancelHandler());
		
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true); 
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		UnityVaadinServlet theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticators, registrationConfiguration);
		context.addServlet(createVaadinServletHolder(theServlet, false), servletPath + "/*");

		
		return context;
	}
}
