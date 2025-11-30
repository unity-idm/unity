/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.as.ActiveOAuthScopeDefinition;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;

@Component
class OAuthTokenEffectiveScopesAttributesCompleter
{
	private OAuthScopesService scopeService;

	OAuthTokenEffectiveScopesAttributesCompleter(OAuthScopesService scopeService)
	{
		this.scopeService = scopeService;
	}

	void fixScopesAttributesIfNeeded(OAuthASProperties config, OAuthToken token)
	{
		Map<String, ActiveOAuthScopeDefinition> activeScopes = scopeService.getActiveScopes(config)
				.stream()
				.collect(Collectors.toMap(ActiveOAuthScopeDefinition::name, s -> s));

		List<RequestedOAuthScope> fixedScopes = new ArrayList<>();

		for (RequestedOAuthScope scope : token.getEffectiveScope())
		{
			ActiveOAuthScopeDefinition originalDef = scope.scopeDefinition();
			if (originalDef.attributes() == null)
			{
				List<String> newAttributes = activeScopes
						.getOrDefault(scope.scope(), ActiveOAuthScopeDefinition.builder()
								.withAttributes(List.of())
								.build())
						.attributes();

				ActiveOAuthScopeDefinition newDefinition = ActiveOAuthScopeDefinition.builder()
						.withName(originalDef.name())
						.withPattern(originalDef.pattern())
						.withDescription(originalDef.description())
						.withAttributes(newAttributes)
						.build();

				fixedScopes.add(new RequestedOAuthScope(scope.scope(), newDefinition, scope.pattern()));
			} else
			{
				fixedScopes.add(scope);
			}
		}

		token.setEffectiveScope(List.copyOf(fixedScopes));
	}
}
