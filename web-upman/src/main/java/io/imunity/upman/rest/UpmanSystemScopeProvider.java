package io.imunity.upman.rest;

import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.api.Scope;
import pl.edu.icm.unity.oauth.api.SystemScopeProvider;

@Component
public class UpmanSystemScopeProvider implements SystemScopeProvider
{
	public static final String API_SCOPE = "sys:upman-api";
	public static final String PROJECT_SCOPE_PREFIX = API_SCOPE + ":";
	public static final String PROJECT_SCOPE_PATTERN = PROJECT_SCOPE_PREFIX + ".+";

	@Override
	public List<Scope> getScopes()
	{
		return List.of(
				Scope.builder().withName(API_SCOPE)
						.withDescription("Access to all projects in the UpMan REST API").build(),
				Scope.builder().withName(PROJECT_SCOPE_PATTERN)
						.withDescription("Access to one project in the UpMan REST API")
						.withPattern(true).build());
	}

	@Override
	public String getId()
	{
		return "UpMan";
	}
}
