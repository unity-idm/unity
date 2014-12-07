/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;

/**
 * Holds a Logout Request along with its destination and relay state. This is used to pass necessary information
 * to perform an in-the-middle request to a session participant in asynchronous bindings.
 * @author K. Benedyczak
 */
public class InterimLogoutRequest
{
	private LogoutRequestDocument request;
	private String relayState;
	private SAMLEndpointDefinition endpoint;
	
	public InterimLogoutRequest(LogoutRequestDocument request, String relayState,
			SAMLEndpointDefinition endpoint)
	{
		super();
		this.request = request;
		this.relayState = relayState;
		this.endpoint = endpoint;
	}

	public LogoutRequestDocument getRequest()
	{
		return request;
	}

	public String getRelayState()
	{
		return relayState;
	}

	public SAMLEndpointDefinition getEndpoint()
	{
		return endpoint;
	}
}
