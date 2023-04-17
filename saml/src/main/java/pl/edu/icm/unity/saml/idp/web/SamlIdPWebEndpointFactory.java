/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import io.imunity.vaadin.auth.VaadinAuthentication;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Factory creating {@link SamlAuthVaadinEndpoint} endpoints.
 * 
 * @author K. Benedyczak
 */
@Component
public class SamlIdPWebEndpointFactory implements EndpointFactory
{
	public static final String NAME = "SAMLWebIdP";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
			"SAML 2 identity provider web endpoint", VaadinAuthentication.NAME,
			Stream.of(new AbstractMap.SimpleEntry<>(SamlAuthVaadinEndpoint.SAML_ENTRY_SERVLET_PATH,
					"SAML 2 identity provider web endpoint"),
					new AbstractMap.SimpleEntry<>(SamlAuthVaadinEndpoint.SAML_META_SERVLET_PATH,
							"Metadata of the SAML 2 identity provider web endpoint"),
					new AbstractMap.SimpleEntry<>(
							SamlAuthVaadinEndpoint.SAML_SLO_ASYNC_SERVLET_PATH,
							"Single Logout web endpoint "
									+ "(supports POST and Redirect bindings)"),
					new AbstractMap.SimpleEntry<>(SamlAuthVaadinEndpoint.SAML_SLO_SOAP_SERVLET_PATH,
							"Single Logout web endpoint (supports SOAP binding)"))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

	@Autowired
	private ObjectFactory<SamlAuthVaadinEndpoint> factory;

	public SamlIdPWebEndpointFactory()
	{

	}

	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return factory.getObject();
	}
}
