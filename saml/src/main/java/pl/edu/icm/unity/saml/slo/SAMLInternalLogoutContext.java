/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 21.11.2014
 * Author: K. Benedyczak <golbi@icm.edu.pl>
 */

package pl.edu.icm.unity.saml.slo;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionParticipant;
import pl.edu.icm.unity.server.api.internal.SessionParticipants;

/**
 * SAML Context for single logout protocol. Quite complicated as the process may happen asynchronously.
 * This context is used to track state during logout, where Unity is session authority.
 * <p>
 * This class is used to keep track of logging out of external SAML session participants, which process
 * can be initialized with SAML request or not (e.g. by logging out directly).
 * 
 * @author K. Benedyczak
 */
public class SAMLInternalLogoutContext extends AbstractSAMLLogoutContext
{
	private String relayState;
	private List<SAMLSessionParticipant> toBeLoggedOut = new ArrayList<SAMLSessionParticipant>();
	private List<SAMLSessionParticipant> loggedOut = new ArrayList<SAMLSessionParticipant>();
	private List<SAMLSessionParticipant> failed = new ArrayList<SAMLSessionParticipant>();
	private SAMLSessionParticipant current;
	private String currentRequestId;
	
	public SAMLInternalLogoutContext(LoginSession loginSession, String excludedFromLogout)
	{
		super(loginSession);
		initialize(excludedFromLogout);
	}

	private void initialize(String excludedFromLogout)
	{
		SessionParticipants participants = SessionParticipants.getFromSession(session.getSessionData());
		for (SessionParticipant p: participants.getParticipants())
		{
			if (SAMLSessionParticipant.TYPE.equals(p.getProtocolType()))
			{
				SAMLSessionParticipant samlP = (SAMLSessionParticipant) p;
				if (excludedFromLogout == null || !excludedFromLogout.equals(samlP.getIdentifier()))
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
}
