/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 21.11.2014
 * Author: K. Benedyczak <golbi@icm.edu.pl>
 */

package pl.edu.icm.unity.saml.slo;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.session.SessionParticipants;
import pl.edu.icm.unity.saml.SAMLSessionParticipant;

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
	private final String relayState;
	private Set<SAMLSessionParticipant> toBeLoggedOut = new LinkedHashSet<>();
	private Set<SAMLSessionParticipant> loggedOut = new LinkedHashSet<>();
	private Set<SAMLSessionParticipant> failed = new LinkedHashSet<>();
	private SAMLSessionParticipant current;
	private String currentRequestId;
	private AsyncLogoutFinishCallback finishCallback;
	
	public SAMLInternalLogoutContext(LoginSession loginSession, String excludedFromLogout,
			AsyncLogoutFinishCallback finishCallback, SessionParticipantTypesRegistry registry,
			String relayState)
	{
		super(loginSession);
		this.finishCallback = finishCallback;
		this.relayState = relayState;
		initialize(excludedFromLogout, registry);
	}

	private void initialize(String excludedFromLogout, SessionParticipantTypesRegistry registry)
	{
		SessionParticipants participants = SessionParticipants.getFromSession(session.getSessionData(),
				registry);
		for (SessionParticipant p: participants.getParticipants())
		{
			if (SAMLSessionParticipant.TYPE.equals(p.getProtocolType()))
			{
				SAMLSessionParticipant samlP = (SAMLSessionParticipant) p;
				if (excludedFromLogout == null || !excludedFromLogout.equals(samlP.getIdentifier()))
				{
					if (toBeLoggedOut.contains(samlP))
					{
						throw new IllegalStateException("Got duplicated SAML session participant, "
								+ "looks like misconfiguration:\n" + samlP
								+ "\nalready have:\n" + toBeLoggedOut);
					}
					toBeLoggedOut.add(samlP);
				}
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

	public Set<SAMLSessionParticipant> getToBeLoggedOut()
	{
		return toBeLoggedOut;
	}

	public Set<SAMLSessionParticipant> getLoggedOut()
	{
		return loggedOut;
	}

	public Set<SAMLSessionParticipant> getFailed()
	{
		return failed;
	}

	public boolean allCorrectlyLoggedOut()
	{
		return failed.isEmpty() && toBeLoggedOut.isEmpty();
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
	
	public AsyncLogoutFinishCallback getFinishCallback()
	{
		return finishCallback;
	}

	/**
	 * Used after the async logout is finished to notify the initiator that the response can be returned.
	 */
	public interface AsyncLogoutFinishCallback
	{
		public void finished(HttpServletResponse response, SAMLInternalLogoutContext finalInternalContext);
	}
}
