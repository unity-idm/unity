/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.group;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.scim.SCIMConstants;
import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.SCIMRestController;
import io.imunity.scim.SCIMRestControllerFactory;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.group.GroupAssemblyService.SCIMGroupResourceAssemblyServiceFactory;
import io.imunity.scim.group.GroupRetrievalService.SCIMGroupRetrievalServiceFactory;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class GroupRestController implements SCIMRestController
{
	public static final String GROUP_LOCATION = "/Groups";

	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, GroupRestController.class);

	private final ObjectMapper mapper = SCIMConstants.MAPPER;

	private final GroupRetrievalService groupRetrievalService;
	private final GroupAssemblyService groupAssemblyService;

	GroupRestController(GroupRetrievalService groupRetrievalService, GroupAssemblyService groupAssemblyService)
	{

		this.groupRetrievalService = groupRetrievalService;
		this.groupAssemblyService = groupAssemblyService;
	}

	@Path(GROUP_LOCATION)
	@GET
	public Response getGroups(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get groups");

		return Response.ok()
				.entity(mapper.writeValueAsString(
						groupAssemblyService.mapToGroupsResource(groupRetrievalService.getGroups())))
				.contentLocation(uriInfo.getRequestUri()).build();
	}

	@Path(GROUP_LOCATION + "/{id}")
	@GET
	public Response getGroup(@PathParam("id") String groupId, @Context UriInfo uriInfo)
			throws EngineException, JsonProcessingException
	{
		log.debug("Get group with id: {}", groupId);
		return Response.ok()
				.entity(mapper.writeValueAsString(
						groupAssemblyService.mapToGroupResource(groupRetrievalService.getGroup(new GroupId(groupId)))))
				.contentLocation(uriInfo.getRequestUri()).build();
	}

	@Component
	static class SCIMGroupRestControllerFactory implements SCIMRestControllerFactory
	{
		private final SCIMGroupRetrievalServiceFactory retServiceFactory;
		private final SCIMGroupResourceAssemblyServiceFactory assemblyServiceFactory;

		@Autowired
		SCIMGroupRestControllerFactory(SCIMGroupRetrievalServiceFactory retServiceFactory,
				SCIMGroupResourceAssemblyServiceFactory assemblyServiceFactory)
		{
			this.retServiceFactory = retServiceFactory;
			this.assemblyServiceFactory = assemblyServiceFactory;
		}

		@Override
		public GroupRestController getController(SCIMEndpointDescription configuration)
		{
			return new GroupRestController(retServiceFactory.getService(configuration),
					assemblyServiceFactory.getService(configuration));
		}
	}

}
