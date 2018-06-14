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

import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
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
	private final Map<String, AuthenticationOption> authentionOptionsByName = new LinkedHashMap<>();
	private Set<String> consumedAuthenticators = new HashSet<>();
	private Set<String> consumedAuthenticatorEntries = new HashSet<>();
	
	AuthenticationOptionsHandler(List<AuthenticationOption> availableAuthentionOptions)
	{
		for (AuthenticationOption ao: availableAuthentionOptions)
			authentionOptionsByName.put(ao.getId(), ao);
	}
	
	void clear()
	{
		consumedAuthenticators.clear();
		consumedAuthenticatorEntries.clear();
	}

	AuthenticationOption getMatchingOption(String spec)
	{
		String authenticatorName = AuthenticationOptionKeyUtils.decodeAuthenticator(spec);
		return authentionOptionsByName.get(authenticatorName);
	}

	VaadinAuthenticationUI getFirstMatchingRetrieval(String spec)
	{
		List<VaadinAuthenticationUI> ret = getMatchingRetrievals(spec);
		return ret.isEmpty() ? null : ret.get(0);
	}

	
	List<VaadinAuthenticationUI> getMatchingRetrievals(String spec)
	{
		AuthenticationOption authNOption = getMatchingOption(spec);
		if (authNOption == null)
			return Collections.emptyList();
		
		String authenticatorOptionName = AuthenticationOptionKeyUtils.decodeAuthenticatorOption(spec);
		if (authenticatorOptionName == null)
		{
			if (!consumedAuthenticators.add(AuthenticationOptionKeyUtils.decodeAuthenticator(spec)))
				return Collections.emptyList();
		}
		VaadinAuthentication firstAuthenticator = (VaadinAuthentication) authNOption.getPrimaryAuthenticator();
		Collection<VaadinAuthenticationUI> optionUIInstances = firstAuthenticator.createUIInstance();
		List<VaadinAuthenticationUI> ret = new ArrayList<>();
		for (VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
		{
			if (!vaadinAuthenticationUI.isAvailable())
				continue;
			String currentKey = AuthenticationOptionKeyUtils.encode(authNOption.getId(), vaadinAuthenticationUI.getId());
			if (consumedAuthenticatorEntries.contains(currentKey))
				continue;
			if (authenticatorOptionName == null || authenticatorOptionName.equals(vaadinAuthenticationUI.getId()))
			{
				ret.add(vaadinAuthenticationUI);
				consumedAuthenticatorEntries.add(currentKey);
			}
		}
		return ret;
	}
	
	Map<AuthenticationOption, List<VaadinAuthenticationUI>> getRemainingRetrievals()
	{
		Map<AuthenticationOption, List<VaadinAuthenticationUI>> ret = new LinkedHashMap<>();
		for (AuthenticationOption authNOption: authentionOptionsByName.values())
		{
			if (consumedAuthenticators.contains(authNOption.getId()))
				continue;
			List<VaadinAuthenticationUI> entries = new ArrayList<>();
			ret.put(authNOption, entries);
			
			VaadinAuthentication firstAuthenticator = (VaadinAuthentication) authNOption.getPrimaryAuthenticator();
			Collection<VaadinAuthenticationUI> optionUIInstances = firstAuthenticator.createUIInstance();
			for (VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
			{
				if (!vaadinAuthenticationUI.isAvailable())
					continue;
				String entryKey = AuthenticationOptionKeyUtils.encode(authNOption.getId(), vaadinAuthenticationUI.getId());
				if (!consumedAuthenticatorEntries.contains(entryKey))
					entries.add(vaadinAuthenticationUI);
			}
		}
		return ret;
	}
}
