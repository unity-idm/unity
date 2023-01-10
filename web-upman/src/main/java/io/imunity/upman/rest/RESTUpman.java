/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path(RESTUpmanEndpoint.V1_PATH)
@PrototypeComponent
public class RESTUpman
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTUpman.class);
	private final ObjectMapper mapper = Constants.MAPPER;
	private final String rootGroup;
	private final RestGroupService restGroupService;


	@Autowired
	public RESTUpman()
	{
	}

	
	@Path("/projects")
	@GET
	public String getProjects() throws EngineException, JsonProcessingException
	{
		log.debug("getProjects query for " + rootGroup);
		List<RestProject> projects = restGroupService.getProjects(rootGroup);
		return mapper.writeValueAsString(projects);
	}

	@Path("/projects/{groupName}")
	@GET
	public String getProject(@PathParam("groupName") String groupName)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProject query for " + groupName);
		RestProject project = restGroupService.getProject(rootGroup, groupName);
		return mapper.writeValueAsString(project);
	}

	@Path("/projects")
	@POST
	public void addProject(String projectJson)
			throws EngineException
	{
		log.info("addProject {}", projectJson);
		RestProjectCreateRequest project = JsonUtil.parse(projectJson, RestProjectCreateRequest.class);
		restGroupService.addProject(rootGroup, project);
	}

	@Path("/projects/{groupName}")
	@PUT
	public void updateProject(@PathParam("groupName") String groupName, String projectJson)
		throws EngineException
	{
		log.info("updateProject {}", projectJson);
		RestProjectUpdateRequest project = JsonUtil.parse(projectJson, RestProjectUpdateRequest.class);
		restGroupService.updateProject(rootGroup, groupName, project);
	}

	@Path("/projects/{groupName}")
	@DELETE
	public void removeProject(@PathParam("groupName") String groupName)
		throws EngineException, JsonProcessingException
	{
		log.info("removeProject {}", groupName);
		restGroupService.removeProject(rootGroup, groupName);
	}

	@Path("/projects/{groupName}/members")
	@GET
	public String getProjectMembers(@PathParam("groupName") String groupName)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMember {}", groupName);
		List<RestProjectMembership> members = restGroupService.getProjectMembers(rootGroup, groupName);
		return mapper.writeValueAsString(members);
	}

	@Path("/projects/{groupName}/members/{userId}")
	@GET
	public String getProjectMember(@PathParam("groupName") String groupName, @PathParam("userId") String userId)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMember {}, {}", groupName, userId);
		RestProjectMembership member = restGroupService.getProjectMember(rootGroup, groupName, Long.parseLong(userId));
		return mapper.writeValueAsString(member);
	}

	@Path("/projects/{groupName}/members/{userId}")
	@DELETE
	public void removeProjectMember(@PathParam("groupName") String groupName, @PathParam("userId") String userId)
		throws EngineException
	{
		log.info("removeProjectMember {}, {}", groupName, userId);
		restGroupService.removeProjectMember(rootGroup, groupName, Long.parseLong(userId));
	}

	@Path("/projects/{groupName}/members/{userId}")
	@POST
	public void addProjectMember(@PathParam("groupName") String groupName, @PathParam("userId") String userId)
		throws EngineException
	{
		log.info("removeProjectMember {}, {}", groupName, userId);
		restGroupService.addProjectMember(rootGroup, groupName, Long.parseLong(userId));
	}

	@Path("/projects/{groupName}/members/{userId}/role")
	@GET
	public String getProjectMemberAuthorizationRole(@PathParam("groupName") String groupName,
	                                           @PathParam("userId") String userId)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMemberAuthorizationRole {}, {}", groupName, userId);
		RestAuthorizationRole role = restGroupService.getProjectAuthorizationRole(rootGroup,
			groupName, Long.parseLong(userId));
		return mapper.writeValueAsString(role);
	}

	@Path("/projects/{groupName}/members/{userId}/role")
	@PUT
	public void addProjectMemberAuthorizationRole(@PathParam("groupName") String groupName,
	                                            @PathParam("userId") String userId, String roleJson)
		throws EngineException
	{
		log.info("addProjectMemberAuthorizationRole {}, {}", groupName, userId);
		RestAuthorizationRole role = JsonUtil.parse(roleJson, RestAuthorizationRole.class);
		restGroupService.setProjectAuthorizationRole(rootGroup,
			groupName, Long.parseLong(userId), role);
	}
}




