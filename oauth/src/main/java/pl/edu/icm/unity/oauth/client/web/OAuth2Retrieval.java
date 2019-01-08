/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.OAuthContextsManagement;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationCapable;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * OAuth2 authn retrieval. It is responsible for browser redirection to the OAuth provider with an authorization
 * request provided by verificator. 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class OAuth2Retrieval extends AbstractCredentialRetrieval<OAuthExchange> 
	implements VaadinAuthentication, ProxyAuthenticationCapable
{
	public static final String NAME = "web-oauth2";
	public static final String DESC = "OAuth2RetrievalFactory.desc";
	public static final String REMOTE_AUTHN_CONTEXT = OAuth2Retrieval.class.getName()+".authnContext";
	private UnityMessageSource msg;
	private OAuthContextsManagement contextManagement;
	private ExecutorsService executorsService;
	private OAuthProxyAuthnHandler oAuthProxyAuthnHandler;
	
	@Autowired
	public OAuth2Retrieval(UnityMessageSource msg, OAuthContextsManagement contextManagement, 
			ExecutorsService executorsService)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.contextManagement = contextManagement;
		this.executorsService = executorsService;
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
	public void setCredentialExchange(CredentialExchange e, String id)
	{
		super.setCredentialExchange(e, id);
		oAuthProxyAuthnHandler = new OAuthProxyAuthnHandler((OAuthExchange) e, id);
	}
	
	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context)
	{
		List<VaadinAuthenticationUI> ret = new ArrayList<>();
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		Set<String> keys = clientProperties.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		for (String key: keys)
		{
			String idpKey = key.substring(OAuthClientProperties.PROVIDERS.length(), 
					key.length()-1);
			ret.add(new OAuth2RetrievalUI(msg, credentialExchange, contextManagement, 
					executorsService, idpKey, key, getAuthenticatorId(), context));
		}
		return ret;
	}
	
	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<OAuth2Retrieval>
	{
		@Autowired
		public Factory(ObjectFactory<OAuth2Retrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, OAuthExchange.class);
		}
	}

	@Override
	public boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath) throws IOException
	{
		return oAuthProxyAuthnHandler.triggerAutomatedAuthentication(httpRequest, httpResponse, endpointPath);
	}

	@Override
	public boolean supportsGrid()
	{
		return true;
	}

	@Override
	public boolean requiresRedirect()
	{
		return true;
	}
}
