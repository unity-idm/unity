/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Vaadin part of the SAML authn, creates the UI component driving the SAML auth, the {@link SAMLRetrievalUI}. 
 * @see SAMLRetrievalFactory
 * 
 * @author K. Benedyczak
 */
public class SAMLRetrieval extends AbstractCredentialRetrieval<SAMLExchange> implements VaadinAuthentication
{
	public static final String REMOTE_AUTHN_CONTEXT = SAMLRetrieval.class.getName() + ".REMOTE_AUTHN_CONTEXT";
	
	private UnityMessageSource msg;
	private SamlContextManagement samlContextManagement;
	
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
	public Collection<VaadinAuthenticationUI> createUIInstance()
	{
		List<VaadinAuthenticationUI> ret = new ArrayList<>();
		SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
		Set<String> allIdps = samlProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		for (String key: allIdps)
			if (samlProperties.isIdPDefinitioncomplete(key))
			{
				Binding binding = samlProperties.getEnumValue(key + 
						SAMLSPProperties.IDP_BINDING, Binding.class);
				if (binding == Binding.HTTP_POST || binding == Binding.HTTP_REDIRECT)
				{
					ret.add(new SAMLRetrievalUI(msg, credentialExchange, 
							samlContextManagement, key));
				}
			}
		return ret;
	}
}










