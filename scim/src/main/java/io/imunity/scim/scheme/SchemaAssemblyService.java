/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.scheme;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.springframework.stereotype.Component;

import io.imunity.scim.common.ListResponse;
import io.imunity.scim.common.Meta;
import io.imunity.scim.common.ResourceType;
import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaWithMapping;

class SchemaAssemblyService
{
	private SCIMEndpointDescription configuration;

	SchemaAssemblyService(SCIMEndpointDescription configuration)
	{
		this.configuration = configuration;
	}

	Optional<SCIMSchemaResource> getSchemaResource(SchemaId schemaId)
	{
		Optional<SchemaWithMapping> schema = configuration.schemas.stream().filter(s -> s.id.equals(schemaId.id)).findAny();
		if (schema.isEmpty() || !schema.get().enable)
		{
			return Optional.empty();
		}
		return Optional.of(mapSingleSchemaResource(schema.get()));
	}

	ListResponse<SCIMSchemaResource> getSchemasResource()
	{
		List<SCIMSchemaResource> schemasResource = configuration.schemas.stream().filter(s -> s.enable)
				.map(s -> mapSingleSchemaResource(s)).collect(Collectors.toList());
		return ListResponse.<SCIMSchemaResource>builder().withResources(schemasResource)
				.withTotalResults(schemasResource.size()).build();
	}

	private SCIMSchemaResource mapSingleSchemaResource(SchemaWithMapping schema)
	{
		return SCIMSchemaResource.builder().withName(schema.name).withDescription(schema.description).withId(schema.id)
				.withMeta(Meta.builder().withResourceType(ResourceType.SCHEMA.getName())
						.withLocation(getSchemaLocation(schema.id)).build())
				.withAttributes(schema.attributesWithMapping.stream()
						.map(a -> mapSingleAttribute(a.attributeDefinition)).collect(Collectors.toList()))
				.build();
	}

	private SCIMAttributeDefinitionResource mapSingleAttribute(AttributeDefinition a)
	{
		return SCIMAttributeDefinitionResource.builder().withName(a.name).withDescription(a.description)
				.withType(a.type.getName()).withMultiValued(a.multiValued).withCaseExact(true).withRequired(false)
				.withMutability(SCIMAttributeMutability.IMMUTABLE.getName())
				.withReturned(SCIMAttributeReturned.DEFAULT.getName())
				.withUniqueness(SCIMAttributeUniqueness.NONE.getName())
				.withSubAttributes(a.subAttributesWithMapping.stream()
						.map(attr -> mapSingleAttribute(attr.attributeDefinition)).collect(Collectors.toList()))
				.build();
	}

	private URI getSchemaLocation(String schema)
	{
		return UriBuilder.fromUri(configuration.baseLocation).path(SchemaRestController.SCHEMA_LOCATION)
				.path(URLEncoder.encode(schema, StandardCharsets.UTF_8)).build();
	}

	@Component
	static class SCIMSchemaAssemblyServiceFactory
	{
		SchemaAssemblyService getService(SCIMEndpointDescription configuration)
		{
			return new SchemaAssemblyService(configuration);
		}
	}
}
