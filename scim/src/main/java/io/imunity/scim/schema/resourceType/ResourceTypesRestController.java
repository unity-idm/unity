/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema.resourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.scim.SCIMConstants;
import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.SCIMRestController;
import io.imunity.scim.SCIMRestControllerFactory;
import io.imunity.scim.common.ListResponse;
import io.imunity.scim.common.Meta;
import io.imunity.scim.common.ResourceType;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;
import io.imunity.scim.group.GroupRestController;
import io.imunity.scim.schema.resourceType.SCIMResourceTypeResource.SchemaExtension;
import io.imunity.scim.user.UserRestController;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class ResourceTypesRestController implements SCIMRestController
{
	static final String RESOURCE_TYPE_LOCATION = "/ResourceTypes";

	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, ResourceTypesRestController.class);
	private final ObjectMapper mapper = SCIMConstants.MAPPER;
	private final SCIMEndpointDescription configuration;

	public ResourceTypesRestController(SCIMEndpointDescription configuration)
	{
		this.configuration = configuration;
	}

	@Path(RESOURCE_TYPE_LOCATION)
	@GET
	public Response getResourceTypes(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get resource types");
		List<SCIMResourceTypeResource> userAndGroupResourceTypes = getUserAndGroupResourceTypes();
		return Response.ok().entity(mapper.writeValueAsString(ListResponse.<SCIMResourceTypeResource>builder()
				.withResources(userAndGroupResourceTypes).withTotalResults(userAndGroupResourceTypes.size()).build()))
				.contentLocation(uriInfo.getRequestUri()).build();

	}

	@Path(RESOURCE_TYPE_LOCATION + "/User")
	@GET
	public Response getUserResourceType(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get resource types");
		return Response.ok().entity(mapper.writeValueAsString(getUserResourceType()))
				.contentLocation(uriInfo.getRequestUri()).build();

	}

	@Path(RESOURCE_TYPE_LOCATION + "/Group")
	@GET
	public Response getGroupResourceType(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get resource types");
		return Response.ok().entity(mapper.writeValueAsString(getGroupResourceType()))
				.contentLocation(uriInfo.getRequestUri()).build();

	}

	private List<SCIMResourceTypeResource> getUserAndGroupResourceTypes()
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

	private SCIMResourceTypeResource getUserResourceType()
	{
		List<SchemaExtension> ext = configuration.schemas.stream().filter(s -> s.type.equals(SchemaType.USER))
				.map(s -> SchemaExtension.builder().withSchema(s.id).withRequired(false).build())
				.collect(Collectors.toList());
		Optional<SchemaWithMapping> core = configuration.schemas.stream()
				.filter(s -> s.type.equals(SchemaType.USER_CORE)).findFirst();
		if (core.isEmpty())
			throw new ResourceTypeNotFoundException("User");

		return SCIMResourceTypeResource.builder().withDescription(ResourceType.USER.getName())
				.withName(ResourceType.USER.getName()).withId(ResourceType.USER.getName())
				.withEndpoint(UriBuilder.fromUri(configuration.baseLocation).path(UserRestController.USER_LOCATION)
						.build().toASCIIString())
				.withSchema(core.get().id).withSchemaExtensions(ext)
				.withMeta(Meta.builder()
						.withLocation(UriBuilder.fromUri(configuration.baseLocation).path(RESOURCE_TYPE_LOCATION)
								.path("User").build())
						.withResourceType(ResourceType.RESOURCE_TYPE.getName()).build())
				.build();
	}

	private SCIMResourceTypeResource getGroupResourceType()
	{
		List<SchemaExtension> ext = configuration.schemas.stream().filter(s -> s.type.equals(SchemaType.GROUP))
				.map(s -> SchemaExtension.builder().withSchema(s.id).withRequired(false).build())
				.collect(Collectors.toList());
		Optional<SchemaWithMapping> core = configuration.schemas.stream()
				.filter(s -> s.type.equals(SchemaType.GROUP_CORE)).findFirst();
		if (core.isEmpty())
			throw new ResourceTypeNotFoundException("Group");

		return SCIMResourceTypeResource.builder().withDescription(ResourceType.GROUP.getName())
				.withName(ResourceType.GROUP.getName()).withId(ResourceType.GROUP.getName())
				.withEndpoint(UriBuilder.fromUri(configuration.baseLocation).path(GroupRestController.GROUP_LOCATION)
						.build().toASCIIString())
				.withSchema(core.get().id).withSchemaExtensions(ext)
				.withMeta(Meta.builder()
						.withLocation(UriBuilder.fromUri(configuration.baseLocation).path(RESOURCE_TYPE_LOCATION)
								.path("Group").build())
						.withResourceType(ResourceType.RESOURCE_TYPE.getName()).build())
				.build();
	}

	@Component
	static class SCIMResourceTypesRestControllerFactory implements SCIMRestControllerFactory
	{

		@Override
		public SCIMRestController getController(SCIMEndpointDescription configuration)
		{
			return new ResourceTypesRestController(configuration);
		}
	}
}
