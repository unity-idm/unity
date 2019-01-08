/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

/**
 * Additional security measure: the user's verification progress is recorded to a 
 * variable stored in session. If user is anyhow trying to perform an operation without fulfilling
 * the previous steps the application will stop him. This shouldn't be possible anyway by Vaadin's design
 * but as Unity's engine authZ is not on guard in password reset case let's go secure.
 * @author K. Benedyczak
 */
public class CredentialResetStateVariable
{
	private static final String SES_NAME = CredentialResetStateVariable.class.getName();
	public enum ResetPrerequisite {CAPTCHA_PROVIDED, STATIC_CHECK_PASSED, CODE_PROVIDED}
	
	public static Set<ResetPrerequisite> get()
	{
		CredentialResetSessionData var = getSessionState();
		return var == null ? new HashSet<>() : new HashSet<>(var.fullfilledChecks);
	}

	public static void assertFullfilled(ResetPrerequisite... required)
	{
		CredentialResetSessionData var = getSessionState();
		if (var == null || !var.fullfilledChecks.containsAll(Sets.newHashSet(required)))
			throw new IllegalStateException("Wrong application security state in credential reset!" +
						" This should never happen.");
	}

	
	public static void record(ResetPrerequisite fullfilled)
	{
		CredentialResetSessionData var = getOrCreateSessionState();
		var.fullfilledChecks.add(fullfilled);
	}
	
	public static void reset()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		WrappedSession session = vSession.getSession();
		session.removeAttribute(SES_NAME);
	}
	
	private static CredentialResetSessionData getOrCreateSessionState()
	{
		CredentialResetSessionData var = getSessionState();
		if (var == null)
		{
			var = new CredentialResetSessionData();
			VaadinSession vSession = VaadinSession.getCurrent();
			WrappedSession session = vSession.getSession();
			session.setAttribute(SES_NAME, var);
		}
		return var;
	}

	private static CredentialResetSessionData getSessionState()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		WrappedSession session = vSession.getSession();
		return (CredentialResetSessionData) session.getAttribute(SES_NAME);
	}

	
	private static class CredentialResetSessionData
	{
		private Set<ResetPrerequisite> fullfilledChecks = new HashSet<>();
	}
}
