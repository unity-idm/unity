/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema.resourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.UriBuilder;

import org.springframework.stereotype.Component;

import io.imunity.scim.common.Meta;
import io.imunity.scim.common.ResourceType;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;
import io.imunity.scim.group.GroupRestController;
import io.imunity.scim.schema.resourceType.SCIMResourceTypeResource.SchemaExtension;
import io.imunity.scim.user.UserRestController;

class ResourceTypeAssemblyService
{
	private final SCIMEndpointDescription configuration;

	ResourceTypeAssemblyService(SCIMEndpointDescription configuration)
	{
		this.configuration = configuration;
	}

	List<SCIMResourceTypeResource> getUserAndGroupResourceTypes()
	{
		List<SCIMResourceTypeResource> types = new ArrayList<>();
		try
		{
			types.add(getUserResourceType());
		} catch (ResourceTypeNotFoundException e)
		{
			// ok
		}

		try
		{
			types.add(getGroupResourceType());
		} catch (ResourceTypeNotFoundException e)
		{
			// ok
		}
		return types;

	}

	SCIMResourceTypeResource getUserResourceType()
	{
		List<SchemaExtension> ext = configuration.schemas.stream().filter(s -> s.type.equals(SchemaType.USER))
				.map(s -> SchemaExtension.builder().withSchema(s.id).withRequired(false).build())
				.collect(Collectors.toList());
		Optional<SchemaWithMapping> core = configuration.schemas.stream()
				.filter(s -> s.type.equals(SchemaType.USER_CORE)).findFirst();
		if (core.isEmpty())
			throw new ResourceTypeNotFoundException(ResourceTypesRestController.USER_RESOURCE);

		return SCIMResourceTypeResource.builder().withDescription(ResourceType.USER.getName())
				.withName(ResourceType.USER.getName()).withId(ResourceType.USER.getName())
				.withEndpoint(UriBuilder.fromUri(configuration.baseLocation).path(UserRestController.USER_LOCATION)
						.build().toASCIIString())
				.withSchema(core.get().id).withSchemaExtensions(ext)
				.withMeta(Meta.builder()
						.withLocation(UriBuilder.fromUri(configuration.baseLocation)
								.path(ResourceTypesRestController.RESOURCE_TYPE_LOCATION)
								.path(ResourceTypesRestController.USER_RESOURCE).build())
						.withResourceType(ResourceType.RESOURCE_TYPE.getName()).build())
				.build();
	}

	SCIMResourceTypeResource getGroupResourceType()
	{
		List<SchemaExtension> ext = configuration.schemas.stream().filter(s -> s.type.equals(SchemaType.GROUP))
				.map(s -> SchemaExtension.builder().withSchema(s.id).withRequired(false).build())
				.collect(Collectors.toList());
		Optional<SchemaWithMapping> core = configuration.schemas.stream()
				.filter(s -> s.type.equals(SchemaType.GROUP_CORE)).findFirst();
		if (core.isEmpty())
			throw new ResourceTypeNotFoundException(ResourceTypesRestController.GROUP_RESOURCE);

		return SCIMResourceTypeResource.builder().withDescription(ResourceType.GROUP.getName())
				.withName(ResourceType.GROUP.getName()).withId(ResourceType.GROUP.getName())
				.withEndpoint(UriBuilder.fromUri(configuration.baseLocation).path(GroupRestController.GROUP_LOCATION)
						.build().toASCIIString())
				.withSchema(core.get().id).withSchemaExtensions(ext)
				.withMeta(Meta.builder()
						.withLocation(UriBuilder.fromUri(configuration.baseLocation)
								.path(ResourceTypesRestController.RESOURCE_TYPE_LOCATION)
								.path(ResourceTypesRestController.GROUP_RESOURCE).build())
						.withResourceType(ResourceType.RESOURCE_TYPE.getName()).build())
				.build();
	}

	@Component
	static class SCIMResourceTypeAssemblyServiceFactory
	{
		ResourceTypeAssemblyService getService(SCIMEndpointDescription configuration)
		{
			return new ResourceTypeAssemblyService(configuration);
		}
	}
}
