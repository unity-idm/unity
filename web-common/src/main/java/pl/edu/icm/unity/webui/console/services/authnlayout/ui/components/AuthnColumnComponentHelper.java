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
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.remote.RemoteAuthnProvidersMultiSelection;

public class AuthnColumnComponentHelper
{
	public static List<AuthenticationOptionKey> getSinglePickerCompatibleAuthnOptions(
			AuthenticatorSupportService authenticatorSupport,
			List<String> availableOptions)
	{
		return getCompatibleAuthnOptions(authenticatorSupport, availableOptions, ui -> true);
	}

	public static List<AuthenticationOptionKey> getGridCompatibleAuthnOptions(AuthenticatorSupportService authenticatorSupport,
			List<String> availableOptions)
	{
		return getCompatibleAuthnOptions(authenticatorSupport, availableOptions, VaadinAuthentication::supportsGrid);
	}

	private static List<AuthenticationOptionKey> getCompatibleAuthnOptions(AuthenticatorSupportService authenticatorSupport,
			List<String> availableOptions, Predicate<VaadinAuthentication> authnUIFilter)
	{
		List<AuthenticationFlow> enabledAuthenticationFlows = authenticatorSupport.resolveAuthenticationFlows(
				availableOptions, VaadinAuthentication.NAME);

		List<AuthenticationOptionKey> authnOptions = new ArrayList<>();
		for (AuthenticationFlow flow: enabledAuthenticationFlows)
			for (AuthenticatorInstance authenticator: flow.getFirstFactorAuthenticators())
			{
				VaadinAuthentication vaadinRetrieval = (VaadinAuthentication) authenticator.getRetrieval();
				if (!authnUIFilter.test(vaadinRetrieval))
					continue;
				Collection<VaadinAuthenticationUI> uiInstances = vaadinRetrieval.createUIInstance(Context.LOGIN);

				if (vaadinRetrieval.isMultiOption())
				{
					for (VaadinAuthenticationUI uiInstance : uiInstances)
					{
						String optionKey = uiInstance.getId();
						authnOptions.add(new AuthenticationOptionKey(
								authenticator.getMetadata().getId(), optionKey));
					}
				}

				if (uiInstances.size() > 0)
				{
					authnOptions.add(new AuthenticationOptionKey(authenticator.getMetadata().getId(),
							AuthenticationOptionKey.ALL_OPTS));
				}

			}
		authnOptions.sort(new RemoteAuthnProvidersMultiSelection.AuthnOptionComparator());
		return authnOptions;
	}
}
