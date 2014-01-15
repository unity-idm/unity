/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.samlidp.FreemarkerHandler;
import pl.edu.icm.unity.samlidp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory creating {@link SamlAuthVaadinEndpoint} endpoints with the UNICORE specific UI.
 * @author K. Benedyczak
 */
@Component
public class SamlUnicoreIdPWebEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/saml2unicoreIdp-web";
	public static final String NAME = "SAMLUnicoreWebIdP";
	

	private EndpointTypeDescription description;
	private ApplicationContext applicationContext;
	private FreemarkerHandler freemarkerHandler;
	
	@Autowired
	public SamlUnicoreIdPWebEndpointFactory(ApplicationContext applicationContext, 
			FreemarkerHandler freemarkerHandler)
	{
		this.applicationContext = applicationContext;
		this.freemarkerHandler = freemarkerHandler;
		
		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(VaadinAuthentication.NAME);
		Map<String,String> paths=new HashMap<String, String>();
		paths.put(SERVLET_PATH,"SAML 2 UNICORE identity provider web endpoint");
		description = new EndpointTypeDescription(NAME, 
				"SAML 2 UNICORE identity provider web endpoint", supportedAuthn,paths);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new SamlAuthETDVaadinEndpoint(getDescription(), applicationContext, freemarkerHandler,
				SamlUnicoreIdPWebUI.class, SERVLET_PATH);
	}
}
