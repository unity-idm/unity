/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web.v8;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import pl.edu.icm.unity.saml.sp.web.SAMLProxyAuthnHandler;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationCapable;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Vaadin part of the SAML authn, creates the UI component driving the SAML auth, the {@link SAMLRetrievalUI}.
 * @see SAMLRetrievalFactory
 */
@PrototypeComponent
public class SAMLRetrievalV8 extends AbstractCredentialRetrieval<SAMLExchange>
		implements VaadinAuthentication, ProxyAuthenticationCapable
{
	public static final String NAME = "web-saml2";
	public static final String DESC = "WebSAMLRetrievalFactory.desc";
	
	private MessageSource msg;
	private SamlContextManagement samlContextManagement;
	private SAMLProxyAuthnHandler proxyAuthnHandler;
	private LogoExposingServiceV8 logoExposingService;

	@Autowired
	public SAMLRetrievalV8(MessageSource msg, SamlContextManagement samlContextManagement,
	                       LogoExposingServiceV8 logoExposingService)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.samlContextManagement = samlContextManagement;
		this.logoExposingService = logoExposingService;
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

				ret.add(new SAMLRetrievalUI(msg, credentialExchange, 
						samlContextManagement, 
						idp.key, context,
						new AuthenticationStepContext(authnStepContext, authenticationOptionKey),
						logoExposingService));
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
	public static class FactoryV8 extends AbstractCredentialRetrievalFactory<SAMLRetrievalV8>
	{
		@Autowired
		public FactoryV8(ObjectFactory<SAMLRetrievalV8> factory)
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

