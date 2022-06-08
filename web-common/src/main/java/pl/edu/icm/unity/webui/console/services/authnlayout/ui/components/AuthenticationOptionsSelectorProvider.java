/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

public class AuthenticationOptionsSelectorProvider
{
	private final AuthenticatorSupportService authenticatorSupport;
	private Map<String, AuthenticationFlow> resolvedFlowCache;

	public AuthenticationOptionsSelectorProvider(AuthenticatorSupportService authenticatorSupport)
	{
		this.authenticatorSupport = authenticatorSupport;
		this.resolvedFlowCache = new HashMap<>();
	}

	public List<AuthenticationOptionsSelector> getSinglePickerCompatibleAuthnSelectors(List<String> availableOptions)
	{
		return getCompatibleAuthnSelectors(availableOptions, ui -> true);
	}

	public List<AuthenticationOptionsSelector> getGridCompatibleAuthnSelectors(
			List<String> availableOptions)
	{
		return getCompatibleAuthnSelectors(availableOptions, VaadinAuthentication::supportsGrid);
	}

	private List<AuthenticationOptionsSelector> getCompatibleAuthnSelectors(List<String> availableOptions,
			Predicate<VaadinAuthentication> authnUIFilter)
	{
		refreshCache(availableOptions);

		List<AuthenticationFlow> enabledAuthenticationFlows = resolvedFlowCache.entrySet().stream()
				.filter(e -> availableOptions.contains(e.getKey())).map(e -> e.getValue()).collect(Collectors.toList());

		List<AuthenticationOptionsSelector> authnOptions = new ArrayList<>();
		for (AuthenticationFlow flow : enabledAuthenticationFlows)
			for (AuthenticatorInstance authenticator : flow.getFirstFactorAuthenticators())
			{
				VaadinAuthentication vaadinRetrieval = (VaadinAuthentication) authenticator.getRetrieval();
				if (!authnUIFilter.test(vaadinRetrieval))
					continue;
				authnOptions.addAll(authenticator.getAuthnOptionSelectors());
			}
		authnOptions.sort(null);
		return authnOptions;
	}

	private void refreshCache(List<String> availableOptions)
	{
		List<String> notResolved = new ArrayList<>(availableOptions);
		notResolved.removeAll(resolvedFlowCache.keySet());
		authenticatorSupport.resolveAuthenticationFlows(notResolved, VaadinAuthentication.NAME)
				.forEach(flow -> resolvedFlowCache.put(flow.getId(), flow));
	}

}
