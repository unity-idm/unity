package io.imunity.upman.rest;

import java.util.List;
import java.util.Iterator;
import java.time.Instant;

import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.api.types.policy.RestPolicyDocumentUpdateRequest;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;

public final class UpmanOpenApi extends OpenApiCustomizer
{
	static void configure(OpenApiFeature feature)
	{
		feature.setTitle("Unity Project Management API");
		feature.setVersion("1");
		feature.setScanKnownConfigLocations(false);
		feature.setScannerClass(UpmanOpenApiScanner.class.getName());
		feature.setCustomizer(new UpmanOpenApi());
	}

	public static OpenAPI export()
	{
		var configuration = new SwaggerConfiguration().readAllResources(true).openAPI(new OpenAPI().info(info()));
		OpenAPI api = new Reader(configuration).read(new UpmanOpenApiScanner().classes());
		new UpmanOpenApi().customize(api);
		return api;
	}

	private static Info info()
	{
		return new Info().title("Unity Project Management API").version("1").description("""
				Manage projects beneath the endpoint's configured root group. API version 1 is independent
				of the Unity product release.

				Direct authentication requires the `sys:ProjectManagementRESTAPIRole` manager role in the
				configured authorization group; project membership and project-local management roles are not
				required. OAuth bearer tokens authorize solely through their granted scopes, independently of
				the client or resource owner's roles or project membership. Tokens require `sys:upman-api` for
				all projects or `sys:upman-api:PROJECT-ID` for one project. Nested scopes use the path relative to the
				endpoint root, for example `sys:upman-api:parent/child`. A project-scoped token sees only
				covered projects and cannot create projects. A parent project's scope does not grant access
				to a nested project as a project in its own right. Scopes use projectId; request paths use
				the returned urlId unchanged.

				Authorization does not override delegation, enableSubprojects or group visibility constraints.
				Public projects and subgroups require public ancestors, including above the endpoint root.

				Authenticators and OAuth authorization server URLs are deployment configuration.
				Requests sent from Scalar use the endpoint's configured authentication.
				""");
	}

	@Override
	public void customize(OpenAPI api)
	{
		// GenericOpenApiContext.modelConverters() modifies Swagger's global singleton in this version.
		// Replace component definitions using a local resolver instead, shared by live and exported documents.
		ModelConverters converters = new ModelConverters();
		List.copyOf(converters.getConverters()).forEach(converters::removeConverter);
		converters.addConverter(new TransportModelResolver());
		for (Class<?> type : List.of(Project.class, ProjectCreateRequest.class,
				ProjectUpdateRequest.class, ProjectId.class, RestPolicyDocument.class,
				RestPolicyDocumentRequest.class, RestPolicyDocumentUpdateRequest.class, PolicyDocumentId.class,
				ProjectMembership.class, GroupMember.class, AuthorizationRole.class,
				InvitationRequest.class, Invitation.class, InvitationOutcome.class, ProjectGroup.class,
				GroupCreateRequest.class, GroupUpdateRequest.class, GroupDelegationRequest.class,
				GroupPath.class, Request.class, FormLink.class, RestRegistrationForm.class, RestEnquiryForm.class))
		{
			var resolved = converters.resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true));
			resolved.referencedSchemas.forEach(api.getComponents()::addSchemas);
		}
		api.getComponents().getSchemas().get("InvitationRequest").addProperty("expiration",
				new io.swagger.v3.oas.models.media.Schema<>().anyOf(List.of(
						new NumberSchema().description("Seconds since the Unix epoch, including fractional seconds"),
						new StringSchema().format("date-time"))));
		api.getComponents().getSchemas().remove("Member");
		UpmanOpenApiDescriptions.apply(api);
		UpmanOpenApiExamples.apply(api);
		api.getComponents().addSecuritySchemes("directAuth", new SecurityScheme()
				.type(SecurityScheme.Type.HTTP).scheme("basic")
				.description("Direct authentication using an endpoint-configured password authenticator."));
		api.getComponents().addSecuritySchemes("oauthBearer", new SecurityScheme()
				.type(SecurityScheme.Type.HTTP).scheme("bearer")
				.description("OAuth token with sys:upman-api or a project-specific sys:upman-api:PROJECT-ID scope. "
						+ "Configure the issuer and token endpoint for the deployed OAuth authenticator."));
		api.setSecurity(List.of(new SecurityRequirement().addList("directAuth"),
				new SecurityRequirement().addList("oauthBearer")));
		api.setInfo(info());
		api.setServers(List.of(new Server().url(".")));
	}

	private static class TransportModelResolver extends ModelResolver
	{
		TransportModelResolver()
		{
			super(UpmanJsonProvider.newMapper());
		}

		@Override
		public io.swagger.v3.oas.models.media.Schema resolve(AnnotatedType type, ModelConverterContext context,
				Iterator<ModelConverter> chain)
		{
			if (type.getType() != null && _mapper.constructType(type.getType()).hasRawClass(Instant.class))
				return new NumberSchema().description("Seconds since the Unix epoch, including fractional seconds");
			return super.resolve(type, context, chain);
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
