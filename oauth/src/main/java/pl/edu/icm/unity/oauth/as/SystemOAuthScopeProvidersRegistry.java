/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.oauth.api.SystemScopeProvider;

@Component("SystemOAuthScopeProvidersRegistry")
public class SystemOAuthScopeProvidersRegistry extends TypesRegistryBase<SystemScopeProvider>
{
	@Autowired
	public SystemOAuthScopeProvidersRegistry(Optional<List<SystemScopeProvider>> typeElements)
	{
		super(typeElements.orElseGet(ArrayList::new));
	}

	@Override
	protected String getId(SystemScopeProvider from)
	{
		return from.getId();
	}
}
