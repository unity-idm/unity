/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.Serializable;

import eu.unicore.samly2.messages.SAMLVerifiableElement;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;


/**
 * Context of a remote SAML authentication. Basically thread safe. As endpoint reconfiguration may occur
 * during authentication configuration valid at the beginning of authentication is stored internally. 
 * @author K. Benedyczak
 */
public class RemoteAuthnContext extends RedirectedAuthnState implements Serializable
{
	private final SAMLSPConfiguration spConfiguration;
	private final TrustedIdPConfiguration idp;
	private final String request;
	private final String requestId;
	private final String returnUrl;

	private Binding responseBinding;
	private String response;
	private SAMLVerifiableElement verifiableResponse;


	public RemoteAuthnContext(TrustedIdPConfiguration idp, SAMLSPConfiguration spConfiguration,
			RedirectedAuthnState baseState,
			String request, String requestId, String returnUrl)
	{
		super(baseState);
		this.idp = idp;
		this.spConfiguration = spConfiguration;
		this.request = request;
		this.requestId = requestId;
		this.returnUrl = returnUrl;
	}

	public synchronized void setResponse(String response, Binding responseBinding, SAMLVerifiableElement verifiableResponse)
	{
		this.response = response;
		this.responseBinding = responseBinding;
		this.verifiableResponse = verifiableResponse;
	}

	public synchronized String getReturnUrl()
	{
		return returnUrl;
	}

	public synchronized String getRequest()
	{
		return request;
	}
	
	public synchronized String getIdpUrl()
	{
		return idp.idpEndpointURL;
	}
	
	public synchronized String getResponse()
	{
		return response;
	}
	
	public synchronized Binding getRequestBinding()
	{
		return idp.binding;
	}

	public synchronized TrustedIdPConfiguration getIdp()
	{
		return idp;
	}

	public synchronized SAMLSPConfiguration getSpConfiguration()
	{
		return spConfiguration;
	}

	public synchronized Binding getResponseBinding()
	{
		return responseBinding;
	}
	
	public synchronized String getRequestId()
	{
		return requestId;
	}
	
	public synchronized String getGroupAttribute()
	{
		return idp.groupMembershipAttribute;
	}
	
	public synchronized String getRegistrationFormForUnknown()
	{
		return idp.registrationForm;
	}
	
	public synchronized boolean isEnableAssociation()
	{
		return idp.enableAccountsAssocation;
		//TODO drop after moving logic to converter
//		String perIdpKey = idpKey + CommonWebAuthnProperties.ENABLE_ASSOCIATION;
//		return samlProperties.isSet(perIdpKey) ? 
//				samlProperties.getBooleanValue(perIdpKey) :
//				samlProperties.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION);
	}
	
	public synchronized SAMLVerifiableElement getVerifiableResponse()
	{
		return verifiableResponse;
	}

	@Override
	public String toString()
	{
		return String.format(
				"SAML RemoteAuthnContext [idp=%s, requestId=%s, initialLoginMachine=%s, relayState=%s]",
				idp.samlId, requestId, getInitialLoginMachine(), getRelayState());
	}
}
