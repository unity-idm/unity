/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.Authenticator;
import pl.edu.icm.unity.webui.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * Helps to find and create UIs matching given spec
 * 
 * @author K. Benedyczak
 */
class AuthenticationOptionsHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationOptionsHandler.class);
	private final Map<String, AuthenticatorWithFlow> authenticatorsByName = new LinkedHashMap<>();

	private Set<String> consumedAuthenticators = new HashSet<>();

	private Set<String> consumedAuthenticatorEntries = new HashSet<>();

	AuthenticationOptionsHandler(List<AuthenticationFlow> availableAuthentionFlows)
	{
		for (AuthenticationFlow ao : availableAuthentionFlows)
			for (Authenticator a: ao.getFirstFactorAuthenticators())
				authenticatorsByName.put(a.getRetrieval().getAuthenticatorId(), 
						new AuthenticatorWithFlow(ao, a));
	}

	void clear()
	{
		consumedAuthenticators.clear();
		consumedAuthenticatorEntries.clear();
	}

	Authenticator getMatchingAuthenticator(String spec)
	{
		String authenticatorName = AuthenticationOptionKeyUtils.decodeAuthenticator(spec);
		return authenticatorsByName.get(authenticatorName).authenticator;
	}

	AuthNOption getFirstMatchingOption(String spec)
	{
		List<AuthNOption> ret = getMatchingAuthnOptions(spec);
		return ret.isEmpty() ? null : ret.get(0);
	}

	List<AuthNOption> getMatchingAuthnOptions(String spec)
	{
		String authenticatorName = AuthenticationOptionKeyUtils.decodeAuthenticator(spec);
		AuthenticatorWithFlow authenticatorWF = authenticatorsByName.get(authenticatorName);
		if (authenticatorWF == null)
		{
			log.warn("There is no authenticator '{}' configured for the endpoint for the '{}' "
					+ "authentication contents entry", authenticatorName, spec);
			return Collections.emptyList();
		}

		String authenticatonOptionName = AuthenticationOptionKeyUtils.decodeOption(spec);
		if (authenticatonOptionName == null)
		{
			if (!consumedAuthenticators.add(AuthenticationOptionKeyUtils.decodeAuthenticator(spec)))
				return Collections.emptyList();
		}
		
		VaadinAuthentication vaadinAuthenticator = (VaadinAuthentication) authenticatorWF.authenticator.getRetrieval();
		Collection<VaadinAuthenticationUI> optionUIInstances = vaadinAuthenticator.createUIInstance();
		List<AuthNOption> ret = new ArrayList<>();
		for (VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
		{
			if (!vaadinAuthenticationUI.isAvailable())
				continue;
			String currentKey = AuthenticationOptionKeyUtils.encode(vaadinAuthenticator.getAuthenticatorId(),
					vaadinAuthenticationUI.getId());
			if (consumedAuthenticatorEntries.contains(currentKey))
				continue;
			if (authenticatonOptionName == null
					|| authenticatonOptionName.equals(vaadinAuthenticationUI.getId()))
			{
				ret.add(new AuthNOption(authenticatorWF.flow, 
						(VaadinAuthentication) authenticatorWF.authenticator.getRetrieval(), 
						vaadinAuthenticationUI));
				consumedAuthenticatorEntries.add(currentKey);
			}
		}
		return ret;
	}

	List<AuthNOption> getRemainingAuthnOptions()
	{
		List<AuthNOption> ret = new ArrayList<>();
		for (AuthenticatorWithFlow authenticatorWF : authenticatorsByName.values())
		{
			if (consumedAuthenticators.contains(authenticatorWF.authenticator.getRetrieval().getAuthenticatorId()))
				continue;

			VaadinAuthentication retrieval = (VaadinAuthentication) authenticatorWF.authenticator.getRetrieval();
			Collection<VaadinAuthenticationUI> optionUIInstances = retrieval.createUIInstance();
			for (VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
			{
				if (!vaadinAuthenticationUI.isAvailable())
					continue;
				String entryKey = AuthenticationOptionKeyUtils.encode(retrieval.getAuthenticatorId(), 
						vaadinAuthenticationUI.getId());
				if (!consumedAuthenticatorEntries.contains(entryKey))
					ret.add(new AuthNOption(authenticatorWF.flow, retrieval, vaadinAuthenticationUI));
			}
		}
		return ret;
	}

	private static class AuthenticatorWithFlow
	{
		final AuthenticationFlow flow;
		final Authenticator authenticator;

		public AuthenticatorWithFlow(AuthenticationFlow flow, Authenticator authenticator)
		{
			this.flow = flow;
			this.authenticator = authenticator;
		}
	}
	
	public static class AuthNOption
	{
		public final AuthenticationFlow flow;
		public final VaadinAuthentication authenticator;
		public final VaadinAuthenticationUI authenticatorUI;

		public AuthNOption(AuthenticationFlow flow, VaadinAuthentication authenticator,
				VaadinAuthenticationUI authenticatorUI)
		{
			this.flow = flow;
			this.authenticator = authenticator;
			this.authenticatorUI = authenticatorUI;
		}
	}
}
