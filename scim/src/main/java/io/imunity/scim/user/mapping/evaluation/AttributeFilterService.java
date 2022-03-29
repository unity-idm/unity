/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.function.Predicate;

import io.imunity.scim.SCIMSystemScopeProvider;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;

class AttributeFilterService
{
	private final SCIMEndpointDescription configuration;

	AttributeFilterService(SCIMEndpointDescription configuration)
	{
		this.configuration = configuration;
	}

	Predicate<AttributeDefinitionWithMapping> getFilter()
	{
		InvocationContext current = InvocationContext.getCurrent();
		if (current.getInvocationMaterial().equals(InvocationMaterial.DIRECT))
		{
			return s -> true;
		} else
		{
			Predicate<AttributeDefinitionWithMapping> attributeFilter = s -> false;
			if (current.getScopes().contains(SCIMSystemScopeProvider.READ_PROFILE_SCOPE))
				attributeFilter = attributeFilter
						.or(s -> !configuration.membershipAttributes.contains(s.attributeDefinition.name));
			if (current.getScopes().contains(SCIMSystemScopeProvider.READ_MEMBERSHIPS_SCOPE))
				attributeFilter = attributeFilter
						.or(s -> configuration.membershipAttributes.contains(s.attributeDefinition.name));
			return attributeFilter;
		}
	}

}
