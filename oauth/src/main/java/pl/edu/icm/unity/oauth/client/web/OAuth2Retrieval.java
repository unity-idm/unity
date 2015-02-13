/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.OAuthContextsManagement;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * OAuth2 authn retrieval. It is responsible for browser redirection to the OAuth provider with an authorization
 * request provided by verificator. 
 * @author K. Benedyczak
 */
public class OAuth2Retrieval implements CredentialRetrieval, VaadinAuthentication
{
	public static final String REMOTE_AUTHN_CONTEXT = OAuth2Retrieval.class.getName()+".authnContext";
	private UnityMessageSource msg;
	private OAuthContextsManagement contextManagement;
	private ExecutorsService executorsService;
	
	private OAuthExchange credentialExchange;
	
	public OAuth2Retrieval(UnityMessageSource msg, OAuthContextsManagement contextManagement, 
			ExecutorsService executorsService)
	{
		this.msg = msg;
		this.contextManagement = contextManagement;
		this.executorsService = executorsService;
	}

	@Override
	public String getBindingName()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (OAuthExchange) e;
	}

	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance()
	{
		List<VaadinAuthenticationUI> ret = new ArrayList<>();
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		Set<String> keys = clientProperties.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		for (String key: keys)
		{
			ret.add(new OAuth2RetrievalUI(msg, credentialExchange, contextManagement, 
					executorsService, key));
		}
		return ret;
	}
}
