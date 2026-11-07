/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import com.fasterxml.jackson.annotation.JsonCreator;

public record RequestedOAuthScope(
		String scope,
		ActiveOAuthScopeDefinition scopeDefinition,
		boolean pattern)
{
	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	static RequestedOAuthScope fromScopeName(String scope)
	{
		return new RequestedOAuthScope(scope, new ActiveOAuthScopeDefinition(scope, "", null, false), false);
	}
}
