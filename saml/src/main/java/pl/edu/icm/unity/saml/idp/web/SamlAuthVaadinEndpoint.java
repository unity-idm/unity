/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.web.filter.ErrorHandler;
import pl.edu.icm.unity.saml.idp.web.filter.SamlGuardFilter;
import pl.edu.icm.unity.saml.idp.web.filter.SamlParseServlet;
import pl.edu.icm.unity.saml.metadata.MetadataProvider;
import pl.edu.icm.unity.saml.metadata.MetadataProviderFactory;
import pl.edu.icm.unity.saml.metadata.MetadataServlet;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;

/**
 * Extends a simple {@link VaadinEndpoint} with configuration of SAML authn filter. Also SAML configuration
 * is parsed here.
 * 
 * @author K. Benedyczak
 */
public class SamlAuthVaadinEndpoint extends VaadinEndpoint
{
	protected SamlIdpProperties samlProperties;
	protected FreemarkerHandler freemarkerHandler;
	protected PKIManagement pkiManagement;
	protected ExecutorsService executorsService;
	protected String samlConsumerPath;
	protected String samlMetadataPath;
	
	public SamlAuthVaadinEndpoint(EndpointTypeDescription type, ApplicationContext applicationContext,
			FreemarkerHandler freemarkerHandler, Class<?> uiClass, String samlUiServletPath, 
			PKIManagement pkiManagement, ExecutorsService executorsService, 
			String samlConsumerPath, String samlMetadataPath)
	{
		super(type, applicationContext, uiClass.getSimpleName(), samlUiServletPath);
		this.freemarkerHandler = freemarkerHandler;
		this.pkiManagement = pkiManagement;
		this.executorsService = executorsService;
		this.samlConsumerPath = samlConsumerPath;
		this.samlMetadataPath = samlMetadataPath;
	}
	
	@Override
	public void setSerializedConfiguration(String properties)
	{
		super.setSerializedConfiguration(properties);
		try
		{
			samlProperties = new SamlIdpProperties(this.properties, pkiManagement);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the SAML Web IdP endpoint's configuration", e);
		}
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
	 	ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getContextAddress());

		String endpointURL = getServletUrl(samlConsumerPath);
		String uiURL = getServletUrl(servletPath);
		Filter samlGuardFilter = new SamlGuardFilter(servletPath, new ErrorHandler(freemarkerHandler));
		context.addFilter(new FilterHolder(samlGuardFilter), servletPath + "/*", 
				EnumSet.of(DispatcherType.REQUEST));
		
		Servlet samlParseServlet = getSamlParseServlet(endpointURL, uiURL);
		ServletHolder samlParseHolder = createServletHolder(samlParseServlet, true);
		context.addServlet(samlParseHolder, samlConsumerPath + "/*");
		
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
		
		CancelHandler cancelHandler = new SamlAuthnCancelHandler(freemarkerHandler,
				description.getContextAddress()+AUTHENTICATION_PATH);
		authenticationServlet.setCancelHandler(cancelHandler);
		
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true); 
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		UnityVaadinServlet theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticators, registrationConfiguration);
		context.addServlet(createVaadinServletHolder(theServlet, false), servletPath + "/*");
		
		if (samlProperties.getBooleanValue(SamlIdpProperties.PUBLISH_METADATA))
		{
			Servlet metadataServlet = getMetadataServlet(endpointURL);
			context.addServlet(createServletHolder(metadataServlet, true), samlMetadataPath + "/*");
		}
		return context;
	}
	
	protected Servlet getSamlParseServlet(String endpointURL, String uiUrl)
	{
		return new SamlParseServlet(samlProperties, 
				endpointURL, uiUrl, new ErrorHandler(freemarkerHandler));
	}
	
	protected Servlet getMetadataServlet(String samlEndpointURL)
	{
		EndpointType ssoPost = EndpointType.Factory.newInstance();
		ssoPost.setLocation(samlEndpointURL);
		ssoPost.setBinding(SAMLConstants.BINDING_HTTP_POST);
		EndpointType ssoRedirect = EndpointType.Factory.newInstance();
		ssoRedirect.setLocation(samlEndpointURL);
		ssoRedirect.setBinding(SAMLConstants.BINDING_HTTP_REDIRECT);

		EndpointType[] endpoints = new EndpointType[] {ssoPost, ssoRedirect};
		
		MetadataProvider provider = MetadataProviderFactory.newIdpInstance(samlProperties, 
				executorsService, endpoints, null);
		return new MetadataServlet(provider);
	}
}
