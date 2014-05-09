/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Produces SAML authN assertion retrievals for the Vaadin authn binding.
 * The retrieval asks the validator for the request, sends it and passes the answer to the validator for the
 * final check up.
 * @author K. Benedyczak
 */
@Component
public class SAMLRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "web-saml2";
	
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private NetworkServer jettyServer;
	@Autowired
	private SharedEndpointManagement sharedEndpointMan;
	@Autowired
	private SamlContextManagement samlContextManagement;

	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "WebSAMLRetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new SAMLRetrieval(msg, jettyServer, sharedEndpointMan, samlContextManagement);
	}

	@Override
	public String getSupportedBinding()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return e instanceof SAMLExchange;
	}
}
