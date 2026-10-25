package io.imunity.rest.admin;

import pl.edu.icm.unity.restadm.RESTAdmin;
import pl.edu.icm.unity.restadm.BulkQueryHandler;
import pl.edu.icm.unity.restadm.BackupRestoreRESTAdmin;
import pl.edu.icm.unity.restadm.PolicyDocumentsRESTAdmin;

import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiScanner;

/** Explicit resource selection avoids classpath scanning across deployed REST endpoints. */
public final class AdminOpenApiScanner implements OpenApiScanner
{
	@Override
	public void setConfiguration(OpenAPIConfiguration configuration)
	{
	}

	@Override
	public Set<Class<?>> classes()
	{
		return Set.of(RESTAdmin.class, BulkQueryHandler.class, BackupRestoreRESTAdmin.class, PolicyDocumentsRESTAdmin.class);
	}

	@Override
	public Map<String, Object> resources()
	{
		return Map.of();
	}
}
