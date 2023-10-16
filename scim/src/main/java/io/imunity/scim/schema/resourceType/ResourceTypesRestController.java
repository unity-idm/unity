/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema.resourceType;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.scim.SCIMConstants;
import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.SCIMRestController;
import io.imunity.scim.SCIMRestControllerFactory;
import io.imunity.scim.common.ListResponse;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class ResourceTypesRestController implements SCIMRestController
{
	static final String RESOURCE_TYPE_LOCATION = "/ResourceTypes";
	static final String USER_RESOURCE = "User";
	static final String GROUP_RESOURCE = "Group";

	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, ResourceTypesRestController.class);
	private final ObjectMapper mapper = SCIMConstants.MAPPER;
	private final  ResourceTypeAssemblyService resourceTypeAssemblyService;

	public ResourceTypesRestController(ResourceTypeAssemblyService resourceTypeAssemblyService)
	{
		this.resourceTypeAssemblyService = resourceTypeAssemblyService;
	}

	@Path(RESOURCE_TYPE_LOCATION)
	@GET
	public Response getResourceTypes(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get resource types");
		List<SCIMResourceTypeResource> userAndGroupResourceTypes = resourceTypeAssemblyService.getUserAndGroupResourceTypes();
		return Response.ok().entity(mapper.writeValueAsString(ListResponse.<SCIMResourceTypeResource>builder()
				.withResources(userAndGroupResourceTypes).withTotalResults(userAndGroupResourceTypes.size()).build()))
				.contentLocation(uriInfo.getRequestUri()).build();

	}

	@Path(RESOURCE_TYPE_LOCATION + "/" + USER_RESOURCE)
	@GET
	public Response getUserResourceType(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get" + USER_RESOURCE + " resource type");
		return Response.ok().entity(mapper.writeValueAsString(resourceTypeAssemblyService.getUserResourceType()))
				.contentLocation(uriInfo.getRequestUri()).build();

	}

	@Path(RESOURCE_TYPE_LOCATION + "/" + GROUP_RESOURCE)
	@GET
	public Response getGroupResourceType(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get" + GROUP_RESOURCE + " resource type");
		return Response.ok().entity(mapper.writeValueAsString(resourceTypeAssemblyService.getGroupResourceType()))
				.contentLocation(uriInfo.getRequestUri()).build();

	}

	@Component
	static class SCIMResourceTypesRestControllerFactory implements SCIMRestControllerFactory
	{

		@Override
		public SCIMRestController getController(SCIMEndpointDescription configuration)
		{
			return new ResourceTypesRestController(new ResourceTypeAssemblyService(configuration));
		}
	}
}
