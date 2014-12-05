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
import pl.edu.icm.unity.server.api.internal.SessionParticipant;
import pl.edu.icm.unity.server.api.internal.SessionParticipants;
import pl.edu.icm.unity.server.registries.SessionParticipantTypesRegistry;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
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
	private String internalRelayState;
	private LogoutRequestType request;
	private LogoutRequestDocument requestDoc;
	private Binding requestBinding;
	private SAMLSessionParticipant initiator;
	
	public SAMLExternalLogoutContext(String localIssuer, LogoutRequestDocument reqDoc, String requesterRelayState, 
			Binding requestBinding, LoginSession loginSession, SessionParticipantTypesRegistry registry)
	{
		super(loginSession);
		this.requestDoc = reqDoc;
		this.request = reqDoc.getLogoutRequest();
		this.requestersRelayState = requesterRelayState;
		this.requestBinding = requestBinding;
		this.localSessionAuthorityId = localIssuer;
		
		initialize(request.getIssuer(), registry);
	}
	
	private void initialize(NameIDType requestIssuer, SessionParticipantTypesRegistry registry)
	{
		SessionParticipants participants = SessionParticipants.getFromSession(session.getSessionData(),
				registry);
		for (SessionParticipant p: participants.getParticipants())
		{
			if (SAMLSessionParticipant.TYPE.equals(p.getProtocolType()))
			{
				SAMLSessionParticipant samlP = (SAMLSessionParticipant) p;
				if (requestIssuer.getStringValue().equals(samlP.getIdentifier()))
				{
					initiator = samlP;
					break;
				}
			}
		}
	}

	public String getInternalRelayState()
	{
		return internalRelayState;
	}

	public void setInternalRelayState(String internalRelayState)
	{
		this.internalRelayState = internalRelayState;
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
