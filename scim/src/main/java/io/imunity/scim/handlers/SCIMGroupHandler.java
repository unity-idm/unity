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
import io.imunity.scim.services.SCIMGroupService;
import io.imunity.scim.services.SCIMGroupService.SCIMGroupServiceFactory;
import io.imunity.scim.types.GroupId;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class SCIMGroupHandler implements SCIMHandler
{
	public static final String SINGLE_GROUP_LOCATION = "/Group";

	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMGroupHandler.class);
	private final SCIMGroupService groupService;
	private final ObjectMapper mapper = Constants.MAPPER;

	SCIMGroupHandler(SCIMGroupService groupService)
	{
		this.groupService = groupService;
	}

	@Path("/Groups")
	@GET
	public String getGroups() throws EngineException, JsonProcessingException
	{
		log.info("Get groups");
		return mapper.writeValueAsString(groupService.getGroups());
	}

	@Path(SINGLE_GROUP_LOCATION + "/{id}")
	@GET
	public String getGroup(@PathParam("id") String groupId) throws EngineException, JsonProcessingException
	{
		log.info("Get group with id: {}", groupId);
		return mapper.writeValueAsString(groupService.getGroup(new GroupId(groupId)));
	}

	@Component
	static class SCIMGroupHandlerFactory implements SCIMHandlerFactory
	{
		private final SCIMGroupServiceFactory serviceFactory;

		@Autowired
		SCIMGroupHandlerFactory(SCIMGroupServiceFactory serviceFactory)
		{
			this.serviceFactory = serviceFactory;
		}

		@Override
		public SCIMHandler getHandler(SCIMEndpointDescription configuration)
		{
			return new SCIMGroupHandler(serviceFactory.getService(configuration));
		}
	}

}
