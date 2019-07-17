/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.endpoints.layout.elements;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.remote.RemoteAuthnProvidersMultiSelection;

/**
 * 
 * @author P.Piernik
 *
 */
public class AuthnColumnElementHelper
{
	public static List<AuthenticationOptionKey> init(AuthenticatorSupportService authenticatorSupport,
			List<String> availableOptions, boolean onlyGrid) throws EngineException
	{

		List<AuthenticatorInstance> athenticators = authenticatorSupport
				.getRemoteAuthenticators(VaadinAuthentication.NAME);
		athenticators.addAll(authenticatorSupport.getLocalAuthenticators(VaadinAuthentication.NAME));

		List<AuthenticationOptionKey> authnOptions = Lists.newArrayList();
		for (AuthenticatorInstance authenticator : athenticators.stream()
				.filter(a -> availableOptions.contains(a.getMetadata().getId()))
				.collect(Collectors.toList()))
		{
			VaadinAuthentication vaadinRetrieval = (VaadinAuthentication) authenticator.getRetrieval();
			List<VaadinAuthenticationUI> uiInstances = vaadinRetrieval
					.createUIInstance(Context.LOGIN).stream().collect(Collectors.toList());

			if (uiInstances.size() > 1)
			{
				for (VaadinAuthenticationUI uiInstance : uiInstances)
				{
					if (onlyGrid)
					{
						if (!isGridCompatible(uiInstance))
							continue;
					}
						
					String optionKey = uiInstance.getId();
					authnOptions.add(new AuthenticationOptionKey(
							authenticator.getMetadata().getId(), optionKey));
				}
			}
			
			if (uiInstances.size() == 1)
			{
				if (onlyGrid)
				{
					if (!isGridCompatible(uiInstances.get(0)))
						continue;
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

	private static boolean isGridCompatible(VaadinAuthenticationUI ui)
	{
		try
		{
			ui.getGridCompatibleComponent();
		} catch (UnsupportedOperationException e)
		{
			return false;
		}
		return true;
	}
}
