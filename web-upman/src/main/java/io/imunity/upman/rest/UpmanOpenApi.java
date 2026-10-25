package io.imunity.upman.rest;

import java.util.List;

import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.api.types.policy.RestPolicyDocumentUpdateRequest;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

final class UpmanOpenApi extends OpenApiCustomizer
{
	static void configure(OpenApiFeature feature)
	{
		feature.setTitle("Unity Project Management API");
		feature.setVersion("1");
		feature.setScanKnownConfigLocations(false);
		feature.setScannerClass(UpmanOpenApiScanner.class.getName());
		feature.setCustomizer(new UpmanOpenApi());
	}

	@Override
	public void customize(OpenAPI api)
	{
		// GenericOpenApiContext.modelConverters() modifies Swagger's global singleton in this version.
		// Replace component definitions using a local resolver instead, shared by live and exported documents.
		ModelConverters converters = new ModelConverters();
		List.copyOf(converters.getConverters()).forEach(converters::removeConverter);
		converters.addConverter(new TransportModelResolver());
		for (Class<?> type : List.of(RestProject.class, RestProjectCreateRequest.class,
				RestProjectUpdateRequest.class, RestProjectId.class, RestPolicyDocument.class,
				RestPolicyDocumentRequest.class, RestPolicyDocumentUpdateRequest.class, RestPolicyDocumentId.class))
		{
			var resolved = converters.resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true));
			resolved.referencedSchemas.forEach(api.getComponents()::addSchemas);
		}
		api.setServers(List.of(new Server().url(".")));
	}

	private static class TransportModelResolver extends ModelResolver
	{
		TransportModelResolver()
		{
			super(UpmanJsonProvider.newMapper());
		}

		@Override
		protected Schema.AccessMode resolveAccessMode(BeanPropertyDefinition property, JavaType type, Schema schema)
		{
			if (schema == null && property.couldSerialize())
				return Schema.AccessMode.READ_WRITE;
			return super.resolveAccessMode(property, type, schema);
		}
	}
}
