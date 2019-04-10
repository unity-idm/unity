/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

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
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
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
	public static final String REMOTE_AUTHN_CONTEXT = SAMLRetrieval.class.getName() + ".REMOTE_AUTHN_CONTEXT";
	
	private UnityMessageSource msg;
	private SamlContextManagement samlContextManagement;
	private SAMLProxyAuthnHandler proxyAuthnHandler;
	
	@Autowired
	public SAMLRetrieval(UnityMessageSource msg, NetworkServer jettyServer, 
			SharedEndpointManagement sharedEndpointMan,
			SamlContextManagement samlContextManagement)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.samlContextManagement = samlContextManagement;
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
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context)
	{
		List<VaadinAuthenticationUI> ret = new ArrayList<>();
		SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
		Set<String> allIdps = samlProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		for (String configKey: allIdps)
			if (samlProperties.isIdPDefinitionComplete(configKey))
			{
				String idpKey = configKey.substring(SAMLSPProperties.IDP_PREFIX.length(), 
						configKey.length()-1);
				Binding binding = samlProperties.getEnumValue(configKey + 
						SAMLSPProperties.IDP_BINDING, Binding.class);
				if (binding == Binding.HTTP_POST || binding == Binding.HTTP_REDIRECT)
				{
					ret.add(new SAMLRetrievalUI(msg, credentialExchange, 
							samlContextManagement, idpKey, 
							configKey, getAuthenticatorId(), context));
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
	public void setCredentialExchange(CredentialExchange e, String id)
	{
		super.setCredentialExchange(e, id);
		proxyAuthnHandler = new SAMLProxyAuthnHandler((SAMLExchange) e, 
				samlContextManagement, id);
	}
	
	@Override
	public boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath) throws IOException
	{
		return proxyAuthnHandler.triggerAutomatedAuthentication(httpRequest, httpResponse, endpointPath);
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










