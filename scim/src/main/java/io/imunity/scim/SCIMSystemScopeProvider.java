/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.oauth.api.Scope;
import pl.edu.icm.unity.oauth.api.SystemScopeProvider;

@Component
public class SCIMSystemScopeProvider implements SystemScopeProvider
{
	private static final String id = "SCIM";

	public static final String READ_PROFILE_SCOPE = "sys:scim:read_profile";
	public static final String READ_MEMBERSHIPS_SCOPE = "sys:scim:read_memberships";

	private final MessageSource msg;

	public SCIMSystemScopeProvider(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public List<Scope> getScopes()
	{
		return List.of(
				Scope.builder().withName(READ_PROFILE_SCOPE)
						.withDescription(msg.getMessage("SCIMScopeProvider.readProfile")).build(),
				Scope.builder().withName(READ_MEMBERSHIPS_SCOPE)
						.withDescription(msg.getMessage("SCIMScopeProvider.readMembership")).build());
	}

	@Override
	public String getId()
	{
		return id;
	}

	public static Set<String> getScopeNames()
	{
		return Set.of(READ_MEMBERSHIPS_SCOPE, READ_PROFILE_SCOPE);
	}
}
