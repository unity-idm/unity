/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.engine.api.utils.HiddenResourcesFilter;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.RoutingServlet;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;
import pl.edu.icm.unity.webui.authn.RememberMeProcessor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * OAuth2 authorization endpoint, Vaadin based.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class OAuthAuthzWebEndpoint extends VaadinEndpoint
{
	public static final String NAME = "OAuth2Authz";

	public static final String OAUTH_UI_SERVLET_PATH = "/oauth2-authz-web-ui";
	public static final String OAUTH_CONSUMER_SERVLET_PATH = "/oauth2-authz";
	public static final String OAUTH_ROUTING_SERVLET_PATH = "/oauth2-authz-web-entry";
	public static final String OAUTH_CONSENT_DECIDER_SERVLET_PATH = "/oauth2-authz-consentdecider";
	
	private OAuthASProperties oauthProperties;
	private FreemarkerAppHandler freemarkerHandler;
	private EntityManagement identitiesManagement;
	private AttributesManagement attributesManagement;
	private PKIManagement pkiManagement;
	private OAuthEndpointsCoordinator coordinator;
	private ASConsentDeciderServletFactory dispatcherServletFactory;
	
	@Autowired
	public OAuthAuthzWebEndpoint(NetworkServer server,
			ApplicationContext applicationContext, FreemarkerAppHandler freemarkerHandler,
			@Qualifier("insecure") EntityManagement identitiesManagement, 
			@Qualifier("insecure") AttributesManagement attributesManagement,
			PKIManagement pkiManagement, OAuthEndpointsCoordinator coordinator,
			ASConsentDeciderServletFactory dispatcherServletFactory, UnityMessageSource msg)
	{
		super(server, msg, applicationContext, OAuthAuthzUI.class.getSimpleName(), OAUTH_UI_SERVLET_PATH);
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
	protected ServletContextHandler getServletContextHandlerOverridable()
	{
	 	ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getEndpoint().getContextAddress());
		
		Servlet samlParseServlet = new OAuthParseServlet(oauthProperties, 
				getServletUrl(OAUTH_ROUTING_SERVLET_PATH), 
				new ErrorHandler(freemarkerHandler), identitiesManagement, 
				attributesManagement); 
		ServletHolder samlParseHolder = createServletHolder(samlParseServlet, true);
		context.addServlet(samlParseHolder, OAUTH_CONSUMER_SERVLET_PATH + "/*");
		
		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		UnityServerConfiguration config = applicationContext.getBean(UnityServerConfiguration.class);
		RememberMeProcessor remeberMeProcessor = applicationContext.getBean(RememberMeProcessor.class);
		
		ServletHolder routingServletHolder = createServletHolder(
				new RoutingServlet(OAUTH_CONSENT_DECIDER_SERVLET_PATH), true);
		context.addServlet(routingServletHolder, OAUTH_ROUTING_SERVLET_PATH + "/*");
		
		Servlet oauthConsentDeciderServlet = dispatcherServletFactory.getInstance(
				OAUTH_UI_SERVLET_PATH, AUTHENTICATION_PATH);
		ServletHolder oauthConsentDeciderHolder = createServletHolder(oauthConsentDeciderServlet, true);
		context.addServlet(oauthConsentDeciderHolder, OAUTH_CONSENT_DECIDER_SERVLET_PATH + "/*");
		
		Filter oauthGuardFilter = new OAuthGuardFilter(new ErrorHandler(freemarkerHandler));
		context.addFilter(new FilterHolder(oauthGuardFilter), OAUTH_ROUTING_SERVLET_PATH + "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		
		context.addFilter(new FilterHolder(new HiddenResourcesFilter(
				Collections.unmodifiableList(Arrays.asList(AUTHENTICATION_PATH, 
						OAUTH_CONSENT_DECIDER_SERVLET_PATH, OAUTH_UI_SERVLET_PATH)))), 
				"/*", EnumSet.of(DispatcherType.REQUEST));
		
		authnFilter = new AuthenticationFilter(
				Collections.singletonList(OAUTH_ROUTING_SERVLET_PATH), 
				AUTHENTICATION_PATH, description.getRealm(), sessionMan, sessionBinder, remeberMeProcessor);
		context.addFilter(new FilterHolder(authnFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		
		proxyAuthnFilter = new ProxyAuthenticationFilter(authenticationFlows, 
				description.getEndpoint().getContextAddress(),
				genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.AUTO_LOGIN));
		context.addFilter(new FilterHolder(proxyAuthnFilter), AUTHENTICATION_PATH + "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		
		contextSetupFilter = new InvocationContextSetupFilter(config, description.getRealm(),
				null, getAuthenticationFlows());
		context.addFilter(new FilterHolder(contextSetupFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		
		EndpointRegistrationConfiguration registrationConfiguration = genericEndpointProperties.getRegistrationConfiguration();
		authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticationFlows,
				registrationConfiguration, properties, 
				getBootstrapHandler4Authn(OAUTH_ROUTING_SERVLET_PATH));
		
		authenticationServlet.setCancelHandler(new OAuthCancelHandler(new OAuthResponseHandler(sessionMan)));
		
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true); 
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticationFlows, registrationConfiguration, properties,
				getBootstrapHandler(OAUTH_ROUTING_SERVLET_PATH));
		context.addServlet(createVaadinServletHolder(theServlet, false), OAUTH_UI_SERVLET_PATH + "/*");

		
		return context;
	}
	
	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<OAuthAuthzWebEndpoint> factory;
		
		private EndpointTypeDescription description = initDescription();
		
		private static EndpointTypeDescription initDescription()
		{
			Map<String, String> paths = new HashMap<>();
			paths.put(OAUTH_CONSUMER_SERVLET_PATH, "OAuth 2 Authorization Grant web endpoint");
			return new EndpointTypeDescription(NAME, 
					"OAuth 2 Server - Authorization Grant endpoint", VaadinAuthentication.NAME, paths);
		}
		
		@Override
		public EndpointTypeDescription getDescription()
		{
			return description;
		}

		@Override
		public EndpointInstance newInstance()
		{
			return factory.getObject();
		}
	}

}
