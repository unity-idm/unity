/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
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
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * Helps to find and create UIs matching given spec
 * 
 * @author K. Benedyczak
 */
public class AuthenticationOptionsHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationOptionsHandler.class);
	private final Map<String, AuthenticatorWithFlow> authenticatorsByName = new LinkedHashMap<>();
	private final String endpoint;

	private Set<String> consumedAuthenticators = new HashSet<>();

	private Set<String> consumedAuthenticatorEntries = new HashSet<>();

	public AuthenticationOptionsHandler(List<AuthenticationFlow> availableAuthentionFlows, String endpoint)
	{
		this.endpoint = endpoint;
		for (AuthenticationFlow ao : availableAuthentionFlows)
			for (AuthenticatorInstance a: ao.getFirstFactorAuthenticators())
			{
				String authenticatorId = a.getRetrieval().getAuthenticatorId();
				if (authenticatorsByName.containsKey(authenticatorId))
					log.warn("Endpoint {} has authenticator {} enabled more then once as a first "
							+ "factor. Most likely it is provisioned from 2 flows. "
							+ "Random one will be used.", endpoint, authenticatorId);
				authenticatorsByName.put(authenticatorId, new AuthenticatorWithFlow(ao, a));
			}
	}

	void clear()
	{
		consumedAuthenticators.clear();
		consumedAuthenticatorEntries.clear();
	}

	AuthenticatorInstance getMatchingAuthenticator(String spec)
	{
		String authenticatorName = AuthenticationOptionKeyUtils.decodeAuthenticator(spec);
		return authenticatorsByName.get(authenticatorName).authenticator;
	}

	AuthNOption getFirstMatchingOption(String spec)
	{
		List<AuthNOption> ret = getMatchingAuthnOptions(spec);
		return ret.isEmpty() ? null : ret.get(0);
	}

	public List<AuthNOption> getMatchingAuthnOptions(String spec)
	{
		String authenticatorName = AuthenticationOptionKeyUtils.decodeAuthenticator(spec);
		AuthenticatorWithFlow authenticatorWF = authenticatorsByName.get(authenticatorName);
		if (authenticatorWF == null)
		{
			log.warn("There is no authenticator '{}' configured for the endpoint {} for the '{}' "
					+ "authentication contents entry", authenticatorName, endpoint, spec);
			return Collections.emptyList();
		}

		String authenticatonOptionName = AuthenticationOptionKeyUtils.decodeOption(spec);
		if (authenticatonOptionName == null)
		{
			if (!consumedAuthenticators.add(AuthenticationOptionKeyUtils.decodeAuthenticator(spec)))
				return Collections.emptyList();
		}
		
		VaadinAuthentication vaadinAuthenticator = (VaadinAuthentication) authenticatorWF.authenticator.getRetrieval();
		Collection<VaadinAuthenticationUI> optionUIInstances = vaadinAuthenticator.createUIInstance(Context.LOGIN);
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
			Collection<VaadinAuthenticationUI> optionUIInstances = retrieval.createUIInstance(Context.LOGIN);
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
		final AuthenticatorInstance authenticator;

		public AuthenticatorWithFlow(AuthenticationFlow flow, AuthenticatorInstance authenticator)
		{
			this.flow = flow;
			this.authenticator = authenticator;
		}
	}
}
