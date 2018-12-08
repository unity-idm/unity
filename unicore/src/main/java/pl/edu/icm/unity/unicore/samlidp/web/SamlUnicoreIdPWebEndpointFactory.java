/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory creating {@link SamlAuthVaadinEndpoint} endpoints with the UNICORE specific UI.
 * @author K. Benedyczak
 */
@Component
public class SamlUnicoreIdPWebEndpointFactory implements EndpointFactory
{
	public static final String NAME = "SAMLUnicoreWebIdP";

	@Autowired
	private ObjectFactory<SamlAuthETDVaadinEndpoint> factory;
	
	private EndpointTypeDescription description;

	public SamlUnicoreIdPWebEndpointFactory()
	{
		Map<String,String> paths=new HashMap<>();
		paths.put(SamlAuthETDVaadinEndpoint.SAML_CONSUMER_SERVLET_PATH,
				"SAML 2 UNICORE identity provider web endpoint");
		paths.put(SamlAuthVaadinEndpoint.SAML_META_SERVLET_PATH, 
				"Metadata of the SAML 2 identity provider web endpoint");
		paths.put(SamlAuthVaadinEndpoint.SAML_SLO_ASYNC_SERVLET_PATH, "Single Logout web endpoint "
				+ "(supports POST and Redirect bindings)");
		paths.put(SamlAuthVaadinEndpoint.SAML_SLO_SOAP_SERVLET_PATH, 
				"Single Logout web endpoint (supports SOAP binding)");
		description = new EndpointTypeDescription(NAME, 
				"SAML 2 UNICORE identity provider web endpoint", VaadinAuthentication.NAME, paths);
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
