package io.imunity.upman.rest;

import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiScanner;

/** Explicit resource selection avoids classpath scanning across deployed REST endpoints. */
public final class UpmanOpenApiScanner implements OpenApiScanner
{
	@Override
	public void setConfiguration(OpenAPIConfiguration configuration)
	{
	}

	@Override
	public Set<Class<?>> classes()
	{
		return Set.of(RESTUpmanController.class);
	}

	@Override
	public Map<String, Object> resources()
	{
		return Map.of();
	}
}
