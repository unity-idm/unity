/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;

import java.util.*;

/**
 * Helps to find and create UIs matching given spec
 */
public class AuthenticationOptionsHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationOptionsHandler.class);
	private final Map<String, AuthenticatorWithFlow> authenticatorsByName = new LinkedHashMap<>();
	private final String endpoint;
	private final AuthenticationRealm realm;
	private final String endpointPath;

	private final Set<String> consumedAuthenticators = new HashSet<>();
	private final Set<String> consumedAuthenticatorEntries = new HashSet<>();

	public AuthenticationOptionsHandler(List<AuthenticationFlow> availableAuthentionFlows, String endpoint,
	                                    AuthenticationRealm realm, String endpointPath)
	{
		this.endpoint = endpoint;
		this.realm = realm;
		this.endpointPath = endpointPath;
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
		AuthenticatorStepContext authenticatorContext = new AuthenticatorStepContext(realm, authenticatorWF.flow, 
				endpointPath, FactorOrder.FIRST);
		Collection<VaadinAuthentication.VaadinAuthenticationUI> optionUIInstances = vaadinAuthenticator.createUIInstance(
				VaadinAuthentication.Context.LOGIN, authenticatorContext);
		List<AuthNOption> ret = new ArrayList<>();
		for (VaadinAuthentication.VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
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
			AuthenticatorStepContext authenticatorContext = new AuthenticatorStepContext(realm, authenticatorWF.flow, 
					endpointPath, FactorOrder.FIRST);
			Collection<VaadinAuthentication.VaadinAuthenticationUI> optionUIInstances = retrieval.createUIInstance(VaadinAuthentication.Context.LOGIN,
					authenticatorContext);
			for (VaadinAuthentication.VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
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
