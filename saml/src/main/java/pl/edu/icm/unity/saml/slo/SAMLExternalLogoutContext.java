/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 21.11.2014
 * Author: K. Benedyczak <golbi@icm.edu.pl>
 */

package pl.edu.icm.unity.saml.slo;

import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestType;

/**
 * SAML Context for single logout protocol. This context is used to track state during logout, 
 * where Unity is session authority when handling a logout initiated by mean of the SAML protocol. Therefore 
 * it preserves the state needed to produce the final response after the whole logout process. 
 * 
 * @author K. Benedyczak
 */
public class SAMLExternalLogoutContext extends AbstractSAMLLogoutContext
{
	private String localSessionAuthorityId;
	private String requestersRelayState;
	private LogoutRequestType request;
	private LogoutRequestDocument requestDoc;
	private Binding requestBinding;
	private SAMLSessionParticipant initiator;
	
	public SAMLExternalLogoutContext(String localIssuer, LogoutRequestDocument reqDoc, String requesterRelayState, 
			Binding requestBinding, LoginSession loginSession)
	{
		super(loginSession);
		this.requestDoc = reqDoc;
		this.request = reqDoc.getLogoutRequest();
		this.requestersRelayState = requesterRelayState;
		this.requestBinding = requestBinding;
		this.localSessionAuthorityId = localIssuer;
	}

	/**
	 * @return null or the relay state which was provided by a session participant 
	 * which requested logout from Unity.
	 */
	public String getRequestersRelayState()
	{
		return requestersRelayState;
	}

	public LogoutRequestType getRequest()
	{
		return request;
	}

	public LogoutRequestDocument getRequestDoc()
	{
		return requestDoc;
	}

	public SAMLSessionParticipant getInitiator()
	{
		return initiator;
	}

	public Binding getRequestBinding()
	{
		return requestBinding;
	}

	public String getLocalSessionAuthorityId()
	{
		return localSessionAuthorityId;
	}

	@Override
	public String toString()
	{
		return "Logout request of " + initiator.getIdentifier();
	}
}
