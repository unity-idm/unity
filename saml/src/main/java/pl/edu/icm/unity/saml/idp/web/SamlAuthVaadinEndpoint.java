/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import eu.unicore.util.configuration.ConfigurationException;

import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.SamlProperties;
import pl.edu.icm.unity.saml.idp.web.filter.SamlParseFilter;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.CancelHandler;

/**
 * Extends a simple {@link VaadinEndpoint} with configuration of SAML authn filter. Also SAML configuration
 * is parsed here.
 * 
 * @author K. Benedyczak
 */
public class SamlAuthVaadinEndpoint extends VaadinEndpoint
{
	protected SamlProperties samlProperties;
	protected FreemarkerHandler freemarkerHandler;
	protected PKIManagement pkiManagement;
	
	public SamlAuthVaadinEndpoint(EndpointTypeDescription type, ApplicationContext applicationContext,
			FreemarkerHandler freemarkerHandler, Class<?> uiClass, String servletPath, 
			PKIManagement pkiManagement)
	{
		super(type, applicationContext, uiClass.getSimpleName(), servletPath);
		this.freemarkerHandler = freemarkerHandler;
		this.pkiManagement = pkiManagement;
	}
	
	@Override
	public void setSerializedConfiguration(String properties)
	{
		super.setSerializedConfiguration(properties);
		try
		{
			samlProperties = new SamlProperties(this.properties, pkiManagement);
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

		String endpointURL = getServletUrl(servletPath);
		Filter samlFilter = getSamlParseFilter(endpointURL); 
		context.addFilter(new FilterHolder(samlFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
		
		AuthenticationFilter authnFilter = new AuthenticationFilter(servletPath, 
				description.getContextAddress()+AUTHENTICATION_PATH);
		context.addFilter(new FilterHolder(authnFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
		
		EndpointRegistrationConfiguration registrationConfiguration = getRegistrationConfiguration();
		UnityVaadinServlet authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticators,
				registrationConfiguration, genericEndpointProperties);
		
		CancelHandler cancelHandler = new SamlAuthnCancelHandler(freemarkerHandler,
				description.getContextAddress()+AUTHENTICATION_PATH);
		authenticationServlet.setCancelHandler(cancelHandler);
		
		ServletHolder authnServletHolder = createServletHolder(authenticationServlet); 
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		UnityVaadinServlet theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticators, registrationConfiguration, genericEndpointProperties);
		context.addServlet(createServletHolder(theServlet), servletPath + "/*");
		
		return context;
	}
	
	protected Filter getSamlParseFilter(String endpointURL)
	{
		return new SamlParseFilter(samlProperties, freemarkerHandler, endpointURL);
	}
}
