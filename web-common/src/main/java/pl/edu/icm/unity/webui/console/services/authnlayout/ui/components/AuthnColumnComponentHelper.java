/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

public class AuthnColumnComponentHelper
{
	public static List<AuthenticationOptionsSelector> getSinglePickerCompatibleAuthnSelectors(
			AuthenticatorSupportService authenticatorSupport,
			List<String> availableOptions)
	{
		return getCompatibleAuthnSelectors(authenticatorSupport, availableOptions, ui -> true);
	}

	public static List<AuthenticationOptionsSelector> getGridCompatibleAuthnSelectors(
			AuthenticatorSupportService authenticatorSupport,
			List<String> availableOptions)
	{
		return getCompatibleAuthnSelectors(authenticatorSupport, availableOptions, VaadinAuthentication::supportsGrid);
	}

	private static List<AuthenticationOptionsSelector> getCompatibleAuthnSelectors(
			AuthenticatorSupportService authenticatorSupport,
			List<String> availableOptions, Predicate<VaadinAuthentication> authnUIFilter)
	{
		List<AuthenticationFlow> enabledAuthenticationFlows = authenticatorSupport.resolveAuthenticationFlows(
				availableOptions, VaadinAuthentication.NAME);

		List<AuthenticationOptionsSelector> authnOptions = new ArrayList<>();
		for (AuthenticationFlow flow: enabledAuthenticationFlows)
			for (AuthenticatorInstance authenticator: flow.getFirstFactorAuthenticators())
			{
				VaadinAuthentication vaadinRetrieval = (VaadinAuthentication) authenticator.getRetrieval();
				if (!authnUIFilter.test(vaadinRetrieval))
					continue;
				//FIXME that should be refactored - we shouldn't create UIs to get a list of available instances
				Collection<VaadinAuthenticationUI> uiInstances = vaadinRetrieval.createUIInstance(
						Context.LOGIN, getMockContext(flow));

				if (vaadinRetrieval.isMultiOption())
				{
					for (VaadinAuthenticationUI uiInstance : uiInstances)
					{
						String optionKey = uiInstance.getId();
						authnOptions.add(new AuthenticationOptionsSelector(
								authenticator.getMetadata().getId(), optionKey));
					}
				}

				if (uiInstances.size() > 0)
				{
					authnOptions.add(AuthenticationOptionsSelector.allForAuthenticator(
							authenticator.getMetadata().getId()));
				}

			}
		authnOptions.sort(null);
		return authnOptions;
	}

	private static AuthenticatorStepContext getMockContext(AuthenticationFlow flow)
	{
		return new AuthenticatorStepContext(null, flow, null, FactorOrder.FIRST);
	}
}
