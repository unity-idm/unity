package io.imunity.rest.admin;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.servers.Server;

public final class AdminOpenApi extends OpenApiCustomizer
{
	public static void configure(OpenApiFeature feature)
	{
		feature.setTitle("Unity Administration API");
		feature.setVersion("1");
		feature.setScanKnownConfigLocations(false);
		feature.setScannerClass(AdminOpenApiScanner.class.getName());
		feature.setCustomizer(new AdminOpenApi());
	}

	public static OpenAPI export()
	{
		var configuration = new SwaggerConfiguration().readAllResources(true).openAPI(new OpenAPI().info(info()));
		OpenAPI api = new Reader(configuration).read(new AdminOpenApiScanner().classes());
		new AdminOpenApi().customize(api);
		return api;
	}

	private static Info info()
	{
		return new Info().title("Unity Administration API").version("1").description("""
				Administration of Unity identities, groups and configuration. API version 1 is independent
				of the Unity product release. Authentication and authorization use the deployed admin endpoint's
				authenticators and the caller's administrative permissions. Live documentation requires the
				same authentication as API operations.
				""");
	}

	@Override
	public void customize(OpenAPI api)
	{
		ModelConverters converters = new ModelConverters();
		List.copyOf(converters.getConverters()).forEach(converters::removeConverter);
		converters.addConverter(new TransportModelResolver());
		for (Class<?> resource : new AdminOpenApiScanner().classes())
			for (Method method : resource.getDeclaredMethods())
			{
				if (method.getAnnotation(jakarta.ws.rs.Path.class) == null)
					continue;
				var resolved = converters.resolveAsResolvedSchema(
						new AnnotatedType(method.getGenericReturnType()).resolveAsRef(true));
				resolved.referencedSchemas.forEach(api.getComponents()::addSchemas);
				for (var parameter : method.getParameters())
					if (parameter.getType().getPackageName().startsWith("io.imunity.rest.api"))
					{
						resolved = converters.resolveAsResolvedSchema(
								new AnnotatedType(parameter.getParameterizedType()).resolveAsRef(true));
						resolved.referencedSchemas.forEach(api.getComponents()::addSchemas);
					}
			}
		api.setInfo(info());
		api.setServers(List.of(new Server().url(".")));
	}

	private static class TransportModelResolver extends ModelResolver
	{
		TransportModelResolver()
		{
			super(AdminJsonProvider.newMapper());
		}

		@Override
		public io.swagger.v3.oas.models.media.Schema resolve(AnnotatedType type, ModelConverterContext context,
				Iterator<ModelConverter> chain)
		{
			if (type.getType() != null)
			{
				JavaType javaType = _mapper.constructType(type.getType());
				if (javaType.hasRawClass(Instant.class))
					return new NumberSchema().description("Seconds since the Unix epoch, including fractional seconds");
				if (Date.class.isAssignableFrom(javaType.getRawClass()))
					return new IntegerSchema().format("int64").description("Milliseconds since the Unix epoch");
			}
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
