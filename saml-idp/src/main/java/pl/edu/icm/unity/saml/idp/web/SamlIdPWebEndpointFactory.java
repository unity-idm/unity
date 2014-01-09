/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory creating {@link SamlAuthVaadinEndpoint} endpoints.
 * @author K. Benedyczak
 */
@Component
public class SamlIdPWebEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/saml2idp-web";
	public static final String NAME = "SAMLWebIdP";
	

	private EndpointTypeDescription description;
	private ApplicationContext applicationContext;
	private FreemarkerHandler freemarkerHandler;
	
	@Autowired
	public SamlIdPWebEndpointFactory(ApplicationContext applicationContext, FreemarkerHandler freemarkerHandler)
	{
		this.applicationContext = applicationContext;
		this.freemarkerHandler = freemarkerHandler;
		
		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(VaadinAuthentication.NAME);
		description = new EndpointTypeDescription(NAME, 
				"SAML 2 identity provider web endpoint", supportedAuthn);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new SamlAuthVaadinEndpoint(getDescription(), applicationContext, freemarkerHandler,
				SamlIdPWebUI.class, SERVLET_PATH);
	}

}
