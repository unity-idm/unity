/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
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
public class RESTUpmanController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTUpmanController.class);
	private final ObjectMapper mapper = Constants.MAPPER;
	private RestProjectService restProjectService;
	private String rootGroup;

	private void init(RestProjectService restProjectService, String rootGroup)
	{
		this.restProjectService = restProjectService;
		this.rootGroup = rootGroup;
	}

	@Path("/projects")
	@GET
	public String getProjects() throws EngineException, JsonProcessingException
	{
		log.debug("getProjects query for " + rootGroup);
		List<RestProject> projects = restProjectService.getProjects();
		return mapper.writeValueAsString(projects);
	}

	@Path("/projects/{groupName}")
	@GET
	public String getProject(@PathParam("groupName") String groupName)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProject query for " + groupName);
		RestProject project = restProjectService.getProject(groupName);
		return mapper.writeValueAsString(project);
	}

	@Path("/projects")
	@POST
	public void addProject(String projectJson)
			throws EngineException
	{
		log.info("addProject {}", projectJson);
		RestProjectCreateRequest project = JsonUtil.parse(projectJson, RestProjectCreateRequest.class);
		restProjectService.addProject(project);
	}

	@Path("/projects/{groupName}")
	@PUT
	public void updateProject(@PathParam("groupName") String groupName, String projectJson)
		throws EngineException
	{
		log.info("updateProject {}", projectJson);
		RestProjectUpdateRequest project = JsonUtil.parse(projectJson, RestProjectUpdateRequest.class);
		restProjectService.updateProject(groupName, project);
	}

	@Path("/projects/{groupName}")
	@DELETE
	public void removeProject(@PathParam("groupName") String groupName)
		throws EngineException, JsonProcessingException
	{
		log.info("removeProject {}", groupName);
		restProjectService.removeProject(groupName);
	}

	@Path("/projects/{groupName}/members")
	@GET
	public String getProjectMembers(@PathParam("groupName") String groupName)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMember {}", groupName);
		List<RestProjectMembership> members = restProjectService.getProjectMembers(groupName);
		return mapper.writeValueAsString(members);
	}

	@Path("/projects/{groupName}/members/{userId}")
	@GET
	public String getProjectMember(@PathParam("groupName") String groupName, @PathParam("userId") String userId)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMember {}, {}", groupName, userId);
		RestProjectMembership member = restProjectService.getProjectMember(groupName, Long.parseLong(userId));
		return mapper.writeValueAsString(member);
	}

	@Path("/projects/{groupName}/members/{userId}")
	@DELETE
	public void removeProjectMember(@PathParam("groupName") String groupName, @PathParam("userId") String userId)
		throws EngineException
	{
		log.info("removeProjectMember {}, {}", groupName, userId);
		restProjectService.removeProjectMember(groupName, Long.parseLong(userId));
	}

	@Path("/projects/{groupName}/members/{userId}")
	@POST
	public void addProjectMember(@PathParam("groupName") String groupName, @PathParam("userId") String userId)
		throws EngineException
	{
		log.info("removeProjectMember {}, {}", groupName, userId);
		restProjectService.addProjectMember(groupName, Long.parseLong(userId));
	}

	@Path("/projects/{groupName}/members/{userId}/role")
	@GET
	public String getProjectMemberAuthorizationRole(@PathParam("groupName") String groupName,
	                                           @PathParam("userId") String userId)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMemberAuthorizationRole {}, {}", groupName, userId);
		RestAuthorizationRole role = restProjectService.getProjectAuthorizationRole(
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
		restProjectService.setProjectAuthorizationRole(
			groupName, Long.parseLong(userId), role);
	}

	@Component
	public static class RESTUpmanControllerFactory
	{
		private final ObjectFactory<RESTUpmanController> factory;
		private final DelegatedGroupManagement delGroupMan;
		private final GroupsManagement groupMan;
		private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
		private final UpmanRestAuthorizationManager authz;
		private final RegistrationsManagement registrationsManagement;
		private final EnquiryManagement enquiryManagement;

		@Autowired
		RESTUpmanControllerFactory(ObjectFactory<RESTUpmanController> factory,
		                           @Qualifier("insecure") DelegatedGroupManagement delGroupMan,
		                           @Qualifier("insecure") GroupsManagement groupMan,
		                           @Qualifier("insecure") GroupDelegationConfigGenerator groupDelegationConfigGenerator,
		                           UpmanRestAuthorizationManager authz,
		                           @Qualifier("insecure") RegistrationsManagement registrationsManagement,
		                           @Qualifier("insecure") EnquiryManagement enquiryManagement)
		{
			this.factory = factory;
			this.delGroupMan = delGroupMan;
			this.groupMan = groupMan;
			this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
			this.authz = authz;
			this.registrationsManagement = registrationsManagement;
			this.enquiryManagement = enquiryManagement;
		}

		public RESTUpmanController newInstance(String rootGroup, String authorizeGroup)
		{
			RESTUpmanController object = factory.getObject();
			object.init(new RestProjectService(
				delGroupMan, groupMan, groupDelegationConfigGenerator, registrationsManagement, enquiryManagement,
				authz, rootGroup, authorizeGroup
			), rootGroup);
			return object;
		}
	}

}




