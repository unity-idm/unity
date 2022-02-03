/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.user;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
import io.imunity.scim.user.SCIMUserResourceAssemblyService.SCIMUserAssemblyServiceFactory;
import io.imunity.scim.user.SCIMUserRetrievalService.SCIMUserRetrievalServiceFactory;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class SCIMUserRestController implements SCIMRestController
{
	public static final String SINGLE_USER_LOCATION = "/User";

	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMUserRestController.class);
	private final SCIMUserRetrievalService userService;
	private final SCIMUserResourceAssemblyService userMapperService;

	private final ObjectMapper mapper = SCIMConstants.MAPPER;

	SCIMUserRestController(SCIMUserRetrievalService userService, SCIMUserResourceAssemblyService userMapperService)
	{
		this.userService = userService;
		this.userMapperService = userMapperService;
	}

	@Path("/Me")
	@GET
	public String getUser() throws EngineException, JsonProcessingException
	{
		log.debug("Get logged user");
		return mapper.writeValueAsString(userService.getLoggedUser());
	}

	@Path("/Users")
	@GET
	public String getUsers() throws EngineException, JsonProcessingException
	{
		log.debug("Get users");
		return mapper.writeValueAsString(userMapperService.mapToListUsersResource(userService.getUsers()));
	}

	@Path(SINGLE_USER_LOCATION + "/{id}")
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
			return new SCIMUserRestController(retServiceFactory.getService(configuration),
					assemblyServiceFactory.getService(configuration));
		}
	}
}
