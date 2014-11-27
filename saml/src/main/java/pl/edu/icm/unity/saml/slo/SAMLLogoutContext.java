/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 21.11.2014
 * Author: K. Benedyczak <golbi@icm.edu.pl>
 */

package pl.edu.icm.unity.saml.slo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionParticipant;
import pl.edu.icm.unity.server.api.internal.SessionParticipants;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestType;

/**
 * SAML Context for single logout protocol. Quite complicated as the process may happen asynchronously.
 * This context is used to track state during logout, where Unity is session authority. 
 * 
 * @author K. Benedyczak
 */
public class SAMLLogoutContext
{
	private String requestersRelayState;
	private String relayState;
	private Date creationTs;
	protected LogoutRequestType request;
	protected LogoutRequestDocument requestDoc;
	protected Binding requestBinding;
	private List<SAMLSessionParticipant> toBeLoggedOut = new ArrayList<SAMLSessionParticipant>();
	private List<SAMLSessionParticipant> loggedOut = new ArrayList<SAMLSessionParticipant>();
	private List<SAMLSessionParticipant> failed = new ArrayList<SAMLSessionParticipant>();
	private SAMLSessionParticipant current;
	private String currentRequestId;
	private SAMLSessionParticipant initiator;
	private LoginSession session;
	private String localSessionAuthorityId;
	
	public SAMLLogoutContext(LogoutRequestDocument reqDoc, LoginSession loginSession,
			String localSessionAuthorityId, String requesterRelayState, Binding requestBinding)
	{
		this.requestDoc = reqDoc;
		this.request = reqDoc.getLogoutRequest();
		creationTs = new Date();
		this.session = loginSession;
		this.localSessionAuthorityId = localSessionAuthorityId;
		this.requestersRelayState = requesterRelayState;
		this.requestBinding = requestBinding;
		initialize();
	}

	private void initialize()
	{
		SessionParticipants participants = SessionParticipants.getFromSession(session.getSessionData());
		String issuer = request.getIssuer().getStringValue();
		for (SessionParticipant p: participants.getParticipants())
		{
			if (SAMLSessionParticipant.TYPE.equals(p.getProtocolType()))
			{
				SAMLSessionParticipant samlP = (SAMLSessionParticipant) p;
				if (issuer.equals(samlP.getIdentifier()))
					this.initiator = samlP;
				else
					toBeLoggedOut.add(samlP);
			}
		}
	}
	
	/**
	 * 
	 * @return null or the relay state which is used by Unity to keep track of 
	 * the async logout processed carried out by it.
	 */
	public String getRelayState()
	{
		return relayState;
	}

	public void setRelayState(String relayState)
	{
		this.relayState = relayState;
	}

	/**
	 * @return null or the relay state which was provided by a session participant 
	 * which requested logout from Unity.
	 */
	public String getRequestersRelayState()
	{
		return requestersRelayState;
	}

	public Date getCreationTs()
	{
		return creationTs;
	}
	
	public LogoutRequestType getRequest()
	{
		return request;
	}

	public LogoutRequestDocument getRequestDoc()
	{
		return requestDoc;
	}

	public List<SAMLSessionParticipant> getToBeLoggedOut()
	{
		return toBeLoggedOut;
	}

	public List<SAMLSessionParticipant> getLoggedOut()
	{
		return loggedOut;
	}

	public List<SAMLSessionParticipant> getFailed()
	{
		return failed;
	}

	public SAMLSessionParticipant getInitiator()
	{
		return initiator;
	}

	public LoginSession getSession()
	{
		return session;
	}
	
	public String getLocalSessionAuthorityId()
	{
		return localSessionAuthorityId;
	}

	public Binding getRequestBinding()
	{
		return requestBinding;
	}

	public SAMLSessionParticipant getCurrent()
	{
		return current;
	}

	public void setCurrent(SAMLSessionParticipant current)
	{
		this.current = current;
	}

	public String getCurrentRequestId()
	{
		return currentRequestId;
	}

	public void setCurrentRequestId(String currentRequestId)
	{
		this.currentRequestId = currentRequestId;
	}

	@Override
	public String toString()
	{
		return "Logout request of " + initiator.getIdentifier() + " for " + session;
	}
}
