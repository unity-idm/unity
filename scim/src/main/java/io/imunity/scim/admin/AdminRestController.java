/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.admin;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.SCIMRestController;
import io.imunity.scim.SCIMRestControllerFactory;
import io.imunity.scim.admin.AdminController.AdminControllerFactory;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class AdminRestController implements SCIMRestController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, AdminRestController.class);
	public static final String CONFIGURATION_LOCATION = "/configuration";

	private final AdminController controller;

	AdminRestController(AdminController controller)
	{
		this.controller = controller;
	}

	@Path(CONFIGURATION_LOCATION + "/exposed-groups")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setExposedGroups(String membershipGroupsConfiguration)
			throws EngineException, JsonProcessingException
	{
		log.debug("setExposedGroups with groups config: " + membershipGroupsConfiguration);
		MembershipGroupsConfiguration parsedMembershipGroupsConfig = parseGroups(membershipGroupsConfiguration);
		controller.updateExposedGroups(parsedMembershipGroupsConfig);
	}
	
	@Path(CONFIGURATION_LOCATION + "/exposed-groups")
	@GET
	public String getExposedGroups() throws EngineException, JsonProcessingException
	{
		String ret = Constants.MAPPER.writeValueAsString(controller.getExposedGroups());
		return ret;
	}

	private MembershipGroupsConfiguration parseGroups(String groupsAsString) throws WrongArgumentException
	{
		if (groupsAsString == null)
			throw new WrongArgumentException("Empty group membership parameter");
		try
		{
			return Constants.MAPPER.readValue(groupsAsString, new TypeReference<MembershipGroupsConfiguration>()
			{
			});
		} catch (IOException e)
		{
			throw new WrongArgumentException("Can not parse request param as a membership groups configuration", e);
		}
	}

	@Component
	static class SCIMAdminRestControllerFactory implements SCIMRestControllerFactory
	{

		private final AdminControllerFactory adminControllerFactory;

		@Autowired
		SCIMAdminRestControllerFactory(AdminControllerFactory adminControllerFactory)
		{
			this.adminControllerFactory = adminControllerFactory;
		}

		@Override
		public SCIMRestController getController(SCIMEndpointDescription configuration)
		{
			return new AdminRestController(adminControllerFactory.getService(configuration));
		}
	}
}
