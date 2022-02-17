/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.user;

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
import io.imunity.scim.user.UserAssemblyService.SCIMUserAssemblyServiceFactory;
import io.imunity.scim.user.UserRetrievalService.SCIMUserRetrievalServiceFactory;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class UserRestController implements SCIMRestController
{
	public static final String USER_LOCATION = "/Users";

	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, UserRestController.class);
	private final UserRetrievalService userService;
	private final UserAssemblyService userMapperService;

	private final ObjectMapper mapper = SCIMConstants.MAPPER;

	UserRestController(UserRetrievalService userService, UserAssemblyService userMapperService)
	{
		this.userService = userService;
		this.userMapperService = userMapperService;
	}

	@Path("/Me")
	@GET
	public Response getUser(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get logged user");
		return Response.ok().entity(mapper.writeValueAsString(userService.getLoggedUser()))
				.contentLocation(uriInfo.getRequestUri()).build();
	}

	@Path(USER_LOCATION)
	@GET
	public Response getUsers(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get users");
		return Response.ok()
				.entity(mapper.writeValueAsString(userMapperService.mapToListUsersResource(userService.getUsers())))
				.contentLocation(uriInfo.getRequestUri()).build();
	}

	@Path(USER_LOCATION + "/{id}")
	@GET
	public String getUser(@PathParam("id") String userId) throws EngineException, JsonProcessingException
	{
		log.debug("Get user with id: {}", userId);
		return mapper
				.writeValueAsString(userMapperService.mapToUserResource(userService.getUser(new PersistentId(userId))));
	}

	@Component
	static class SCIMUserRestControllerFactory implements SCIMRestControllerFactory
	{
		private final SCIMUserRetrievalServiceFactory retServiceFactory;
		private final SCIMUserAssemblyServiceFactory assemblyServiceFactory;

		@Autowired
		SCIMUserRestControllerFactory(SCIMUserRetrievalServiceFactory retServiceFactory,
				SCIMUserAssemblyServiceFactory assemblyServiceFactory)
		{
			this.retServiceFactory = retServiceFactory;
			this.assemblyServiceFactory = assemblyServiceFactory;
		}

		@Override
		public SCIMRestController getController(SCIMEndpointDescription configuration)
		{
			return new UserRestController(retServiceFactory.getService(configuration),
					assemblyServiceFactory.getService(configuration));
		}
	}
}
