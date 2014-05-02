/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.client.OAuthContextsManagement;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;


/**
 * Produces {@link OAuth2Retrieval}s for the Vaadin web binding.
 * @author K. Benedyczak
 */
@Component
public class OAuthh2RetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "web-oauth2";
	
	@Autowired
	private UnityMessageSource msg;
	
	@Autowired
	private OAuthContextsManagement contextManagement;
	
	@Autowired
	private ExecutorsService executorsService;
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "OAuth2RetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new OAuth2Retrieval(msg, contextManagement, executorsService);
	}

	@Override
	public String getSupportedBinding()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return e instanceof OAuthExchange;
	}

}
