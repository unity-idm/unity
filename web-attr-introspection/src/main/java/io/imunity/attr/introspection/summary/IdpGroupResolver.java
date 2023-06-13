/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.summary;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

class IdpGroupResolver
{
	private final AuthenticatorSupportService authenticatorSupportService;

	IdpGroupResolver(AuthenticatorSupportService authenticatorSupportService)
	{
		this.authenticatorSupportService = authenticatorSupportService;
	}

	Optional<String> resolveGroupForIdp(String idp) throws EngineException
	{
		Map<String, Optional<String>> groups = null;
		groups = getGroups();
		return groups.get(idp) == null ? Optional.empty() : groups.get(idp);
	}

	private Map<String, Optional<String>> getGroups() throws EngineException
	{
		return authenticatorSupportService.getRemoteAuthenticators(VaadinAuthentication.NAME).stream()
				.map(a -> a.extractIdPs()).flatMap(List::stream)
				.collect(Collectors.toMap(i -> i.id, i -> i, (i1, i2) -> i1)).values().stream().collect(Collectors
						.toMap(i -> i.id, i -> i.group.isEmpty() ? Optional.empty() : Optional.of(i.group.get().id)));
	}

}
