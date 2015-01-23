/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Arrays;
import java.util.Collections;
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
import pl.edu.icm.unity.server.utils.HiddenResourcesFilter;
import pl.edu.icm.unity.server.utils.RoutingServlet;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * OAuth2 authorization endpoint, Vaadin based.
 * @author K. Benedyczak
 */
public class OAuthAuthzWebEndpoint extends VaadinEndpoint
{
	public static final String OAUTH_UI_SERVLET_PATH = "/oauth2-authz-web-ui";
	public static final String OAUTH_CONSUMER_SERVLET_PATH = "/oauth2-authz";
	public static final String OAUTH_ROUTING_SERVLET_PATH = "/oauth2-authz-web-entry";
	public static final String OAUTH_CONSENT_DECIDER_SERVLET_PATH = "/oauth2-authz-consentdecider";
	
	private OAuthASProperties oauthProperties;
	private FreemarkerHandler freemarkerHandler;
	private IdentitiesManagement identitiesManagement;
	private AttributesManagement attributesManagement;
	private PKIManagement pkiManagement;
	private OAuthEndpointsCoordinator coordinator;
	private ASConsentDeciderServletFactory dispatcherServletFactory;
	
	public OAuthAuthzWebEndpoint(EndpointTypeDescription type,
			ApplicationContext applicationContext, FreemarkerHandler freemarkerHandler,
			IdentitiesManagement identitiesManagement, AttributesManagement attributesManagement,
			PKIManagement pkiManagement, OAuthEndpointsCoordinator coordinator,
			ASConsentDeciderServletFactory dispatcherServletFactory)
	{
		super(type, applicationContext, OAuthAuthzUI.class.getSimpleName(), OAUTH_UI_SERVLET_PATH);
		this.freemarkerHandler = freemarkerHandler;
		this.attributesManagement = attributesManagement;
		this.identitiesManagement = identitiesManagement;
		this.pkiManagement = pkiManagement;
		this.coordinator = coordinator;
		this.dispatcherServletFactory = dispatcherServletFactory;
	}

	@Override
	public void setSerializedConfiguration(String properties)
	{
		super.setSerializedConfiguration(properties);
		try
		{
			oauthProperties = new OAuthASProperties(this.properties, pkiManagement, 
					getServletUrl(OAUTH_CONSUMER_SERVLET_PATH));
			coordinator.registerAuthzEndpoint(oauthProperties.getValue(OAuthASProperties.ISSUER_URI), 
					getServletUrl(OAUTH_CONSUMER_SERVLET_PATH));
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
		
		String endpointURL = getServletUrl(OAUTH_CONSUMER_SERVLET_PATH);
		Servlet samlParseServlet = new OAuthParseServlet(oauthProperties, endpointURL, 
				getServletUrl(OAUTH_ROUTING_SERVLET_PATH), 
				new ErrorHandler(freemarkerHandler), identitiesManagement, 
				attributesManagement); 
		ServletHolder samlParseHolder = createServletHolder(samlParseServlet, true);
		context.addServlet(samlParseHolder, OAUTH_CONSUMER_SERVLET_PATH + "/*");
		
		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		UnityServerConfiguration config = applicationContext.getBean(UnityServerConfiguration.class);
		
		ServletHolder routingServletHolder = createServletHolder(
				new RoutingServlet(OAUTH_CONSENT_DECIDER_SERVLET_PATH), true);
		context.addServlet(routingServletHolder, OAUTH_ROUTING_SERVLET_PATH + "/*");
		
		Servlet oauthConsentDeciderServlet = dispatcherServletFactory.getInstance(OAUTH_UI_SERVLET_PATH);
		ServletHolder oauthConsentDeciderHolder = createServletHolder(oauthConsentDeciderServlet, true);
		context.addServlet(oauthConsentDeciderHolder, OAUTH_CONSENT_DECIDER_SERVLET_PATH + "/*");
		
		Filter oauthGuardFilter = new OAuthGuardFilter(new ErrorHandler(freemarkerHandler));
		context.addFilter(new FilterHolder(oauthGuardFilter), OAUTH_ROUTING_SERVLET_PATH + "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		
		context.addFilter(new FilterHolder(new HiddenResourcesFilter(
				Collections.unmodifiableList(Arrays.asList(AUTHENTICATION_PATH, 
						OAUTH_CONSENT_DECIDER_SERVLET_PATH, OAUTH_UI_SERVLET_PATH)))), 
				"/*", EnumSet.of(DispatcherType.REQUEST));
		
		AuthenticationFilter authnFilter = new AuthenticationFilter(
				Collections.singletonList(OAUTH_ROUTING_SERVLET_PATH), 
				AUTHENTICATION_PATH, description.getRealm(), sessionMan, sessionBinder);
		context.addFilter(new FilterHolder(authnFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		
		contextSetupFilter = new InvocationContextSetupFilter(config, description.getRealm(),
				getServletUrl(""));
		context.addFilter(new FilterHolder(contextSetupFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		
		EndpointRegistrationConfiguration registrationConfiguration = getRegistrationConfiguration();
		UnityVaadinServlet authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticators,
				registrationConfiguration, properties);
		
		authenticationServlet.setCancelHandler(new OAuthCancelHandler());
		
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true); 
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		UnityVaadinServlet theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticators, registrationConfiguration, properties);
		context.addServlet(createVaadinServletHolder(theServlet, false), OAUTH_UI_SERVLET_PATH + "/*");

		
		return context;
	}
}
