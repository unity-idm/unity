/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationCapable;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Vaadin part of the SAML authn, creates the UI component driving the SAML auth, the {@link SAMLRetrievalUI}. 
 * @see SAMLRetrievalFactory
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class SAMLRetrieval extends AbstractCredentialRetrieval<SAMLExchange> 
		implements VaadinAuthentication, ProxyAuthenticationCapable
{
	public static final String NAME = "web-saml2";
	public static final String DESC = "WebSAMLRetrievalFactory.desc";
	
	private MessageSource msg;
	private SamlContextManagement samlContextManagement;
	private SAMLProxyAuthnHandler proxyAuthnHandler;
	private URIAccessService uriAccessService;
	
	@Autowired
	public SAMLRetrieval(MessageSource msg,
			SamlContextManagement samlContextManagement, URIAccessService uriAccessService)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.samlContextManagement = samlContextManagement;
		this.uriAccessService = uriAccessService;
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";	
	}

	@Override
	public void setSerializedConfiguration(String source)
	{
	}

	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context, AuthenticatorStepContext authnStepContext)
	{
		List<VaadinAuthenticationUI> ret = new ArrayList<>();
		TrustedIdPs trustedIdps = credentialExchange.getTrustedIdPs();
		for (TrustedIdPConfiguration idp: trustedIdps.getAll())
		{
			TrustedIdPKey idpKey = idp.key;
			Binding binding = idp.binding;
			if (binding == Binding.HTTP_POST || binding == Binding.HTTP_REDIRECT)
			{
				AuthenticationOptionKey authenticationOptionKey = 
						new AuthenticationOptionKey(getAuthenticatorId(), idpKey.asString());

				ret.add(new SAMLRetrievalUI(msg, uriAccessService, credentialExchange, 
						samlContextManagement, 
						idp.key, context,
						new AuthenticationStepContext(authnStepContext, authenticationOptionKey)));
			}
		}
		return ret;
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
	public void setCredentialExchange(CredentialExchange e, String id)
	{
		super.setCredentialExchange(e, id);
		proxyAuthnHandler = new SAMLProxyAuthnHandler((SAMLExchange) e, 
				samlContextManagement, id);
	}
	
	@Override
	public boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath, AuthenticatorStepContext context) throws IOException
	{
		return proxyAuthnHandler.triggerAutomatedAuthentication(httpRequest, httpResponse, endpointPath, context);
	}

	@Override
	public void triggerAutomatedUIAuthentication(VaadinAuthenticationUI authenticatorUI)
	{
		SAMLRetrievalUI ui = (SAMLRetrievalUI) authenticatorUI;
		ui.startLogin();
	}
	
	@Override
	public void destroy()
	{
		credentialExchange.destroy();
	}
	
	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<SAMLRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<SAMLRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, SAMLExchange.ID);
		}
	}

	@Override
	public boolean requiresRedirect()
	{
		return true;
	}
}

