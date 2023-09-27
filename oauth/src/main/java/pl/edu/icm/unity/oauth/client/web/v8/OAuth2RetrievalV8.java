/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web.v8;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationCapable;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * OAuth2 authn retrieval. It is responsible for browser redirection to the OAuth provider with an authorization
 * request provided by verificator. 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class OAuth2RetrievalV8 extends AbstractCredentialRetrieval<OAuthExchange>
	implements VaadinAuthentication, ProxyAuthenticationCapable
{
	public static final String NAME = "web-oauth2";
	public static final String DESC = "OAuth2RetrievalFactory.desc";
	private MessageSource msg;
	private ImageAccessService imageService;
	private ExecutorsService executorsService;
	private OAuthProxyAuthnHandlerEE8 oAuthProxyAuthnHandler;
	
	@Autowired
	public OAuth2RetrievalV8(MessageSource msg, ImageAccessService imageService,
	                         ExecutorsService executorsService)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.executorsService = executorsService;
		this.imageService = imageService;
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
		oAuthProxyAuthnHandler = new OAuthProxyAuthnHandlerEE8((OAuthExchange) e, id);
	}
	
	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context, AuthenticatorStepContext authenticatorContext)
	{
		List<VaadinAuthenticationUI> ret = new ArrayList<>();
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		Set<String> keys = clientProperties.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		for (String key: keys)
		{
			String idpKey = key.substring(OAuthClientProperties.PROVIDERS.length(), 
					key.length()-1);
			AuthenticationOptionKey authenticationOptionKey = 
					new AuthenticationOptionKey(getAuthenticatorId(), idpKey);
			ret.add(new OAuth2RetrievalUI(msg, imageService, credentialExchange,  
					executorsService, key, context,
					new AuthenticationStepContext(authenticatorContext, authenticationOptionKey)));
		}
		return ret;
	}
	
	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<OAuth2RetrievalV8>
	{
		@Autowired
		public Factory(ObjectFactory<OAuth2RetrievalV8> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, OAuthExchange.ID);
		}
	}

	@Override
	public boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath, AuthenticatorStepContext context) throws IOException
	{
		return oAuthProxyAuthnHandler.triggerAutomatedAuthentication(httpRequest, httpResponse, endpointPath, context);
	}

	@Override
	public boolean supportsGrid()
	{
		return true;
	}

	@Override
	public boolean isMultiOption()
	{
		return true;
	}

	@Override
	public boolean requiresRedirect()
	{
		return true;
	}

	@Override
	public void triggerAutomatedUIAuthentication(VaadinAuthenticationUI authenticatorUI)
	{
		OAuth2RetrievalUI oauthUI = (OAuth2RetrievalUI) authenticatorUI;
		oauthUI.startLogin();
	}
}
