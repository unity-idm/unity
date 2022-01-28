/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.handlers;

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

import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.services.SCIMUserService;
import io.imunity.scim.services.SCIMUserService.SCIMUserServiceFactory;
import io.imunity.scim.types.PersistentId;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class SCIMUserHandler implements SCIMHandler
{
	public static final String SINGLE_USER_LOCATION = "/User";

	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMUserHandler.class);
	private final SCIMUserService userService;
	private final ObjectMapper mapper = Constants.MAPPER;

	SCIMUserHandler(SCIMUserService userService)
	{
		this.userService = userService;
	}

	@Path("/Me")
	@GET
	public String getUser() throws EngineException, JsonProcessingException
	{
		log.info("Get logged user");
		return mapper.writeValueAsString(userService.getLoggedUser());
	}

	@Path("/Users")
	@GET
	public String getUsers() throws EngineException, JsonProcessingException
	{
		log.info("Get users");
		return mapper.writeValueAsString(userService.getUsers());
	}

	@Path(SINGLE_USER_LOCATION + "/{id}")
	@GET
	public String getUser(@PathParam("id") String userId) throws EngineException, JsonProcessingException
	{
		log.info("Get user with id: {}", userId);
		return mapper.writeValueAsString(userService.getUser(new PersistentId(userId)));
	}

	@Component
	static class SCIMUserHandlerFactory implements SCIMHandlerFactory
	{
		private final SCIMUserServiceFactory serviceFactory;

		@Autowired
		SCIMUserHandlerFactory(SCIMUserServiceFactory serviceFactory)
		{
			this.serviceFactory = serviceFactory;
		}

		@Override
		public SCIMHandler getHandler(SCIMEndpointDescription configuration)
		{
			return new SCIMUserHandler(serviceFactory.getService(configuration));
		}
	}
}
