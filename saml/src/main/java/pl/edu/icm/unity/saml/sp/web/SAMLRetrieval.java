/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.net.URL;

import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.server.api.SharedEndpointManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Vaadin part of the SAML authn, creates the UI component driving the SAML auth, the {@link SAMLRetrievalUI}. 
 * @see SAMLRetrievalFactory
 * 
 * @author K. Benedyczak
 */
public class SAMLRetrieval implements CredentialRetrieval, VaadinAuthentication
{
	public static final String REMOTE_AUTHN_CONTEXT = SAMLRetrieval.class.getName() + ".REMOTE_AUTHN_CONTEXT";
	
	private UnityMessageSource msg;
	private SAMLExchange credentialExchange;
	private URL baseAddress;
	private String baseContext;
	private SamlContextManagement samlContextManagement;
	
	public SAMLRetrieval(UnityMessageSource msg, NetworkServer jettyServer, 
			SharedEndpointManagement sharedEndpointMan,
			SamlContextManagement samlContextManagement)
	{
		this.msg = msg;
		this.baseAddress = jettyServer.getAdvertisedAddress();
		this.baseContext = sharedEndpointMan.getBaseContextPath();
		this.samlContextManagement = samlContextManagement;
	}

	@Override
	public String getBindingName()
	{
		return VaadinAuthentication.NAME;
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
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (SAMLExchange) e;
	}


	@Override
	public VaadinAuthenticationUI createUIInstance()
	{
		return new SAMLRetrievalUI(msg, credentialExchange, baseAddress, baseContext,
				samlContextManagement);
	}
}










