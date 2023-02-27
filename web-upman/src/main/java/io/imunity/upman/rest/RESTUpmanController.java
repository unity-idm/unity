/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;

import javax.ws.rs.BadRequestException;
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
	private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
	private RestProjectService restProjectService;
	private String rootGroup;

	private void init(RestProjectService restProjectService, String rootGroup)
	{
		this.restProjectService = restProjectService;
		this.rootGroup = rootGroup;
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NON_PRIVATE);
	}

	@Path("/projects")
	@GET
	public String getProjects() throws EngineException, JsonProcessingException
	{
		log.debug("getProjects query for " + rootGroup);
		List<RestProject> projects = restProjectService.getProjects();
		return mapper.writeValueAsString(projects);
	}

	@Path("/projects/{project-id}")
	@GET
	public String getProject(@PathParam("project-id") String projectId)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProject query for " + projectId);
		RestProject project = restProjectService.getProject(projectId);
		return mapper.writeValueAsString(project);
	}

	@Path("/projects")
	@POST
	public String addProject(String projectJson)
		throws EngineException, JsonProcessingException
	{
		log.info("addProject {}", projectJson);
		RestProjectCreateRequest project = parse(projectJson, RestProjectCreateRequest.class);
		RestProjectId restProjectId = restProjectService.addProject(project);
		return mapper.writeValueAsString(restProjectId);
	}

	@Path("/projects/{project-id}")
	@PUT
	public void updateProject(@PathParam("project-id") String projectId, String projectJson)
		throws EngineException
	{
		log.info("updateProject {}", projectJson);
		RestProjectUpdateRequest project = parse(projectJson, RestProjectUpdateRequest.class);
		restProjectService.updateProject(projectId, project);
	}

	@Path("/projects/{project-id}")
	@DELETE
	public void removeProject(@PathParam("project-id") String projectId)
		throws EngineException, JsonProcessingException
	{
		log.info("removeProject {}", projectId);
		restProjectService.removeProject(projectId);
	}

	@Path("/projects/{project-id}/members")
	@GET
	public String getProjectMembers(@PathParam("project-id") String projectId)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMember {}", projectId);
		List<RestProjectMembership> members = restProjectService.getProjectMembers(projectId);
		return mapper.writeValueAsString(members);
	}

	@Path("/projects/{project-id}/members/{userId}")
	@GET
	public String getProjectMember(@PathParam("project-id") String projectId, @PathParam("userId") String email)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMember {}, {}", projectId, email);
		RestProjectMembership member = restProjectService.getProjectMember(projectId, email);
		return mapper.writeValueAsString(member);
	}

	@Path("/projects/{project-id}/members/{userId}")
	@DELETE
	public void removeProjectMember(@PathParam("project-id") String projectId, @PathParam("userId") String userId)
		throws EngineException
	{
		log.info("removeProjectMember {}, {}", projectId, userId);
		restProjectService.removeProjectMember(projectId, userId);
	}

	@Path("/projects/{project-id}/members/{userId}")
	@POST
	public void addProjectMember(@PathParam("project-id") String projectId, @PathParam("userId") String userId)
		throws EngineException
	{
		log.info("removeProjectMember {}, {}", projectId, userId);
		restProjectService.addProjectMember(projectId, userId);
	}

	@Path("/projects/{project-id}/members/{userId}/role")
	@GET
	public String getProjectMemberAuthorizationRole(@PathParam("project-id") String projectId,
	                                           @PathParam("userId") String userId)
		throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMemberAuthorizationRole {}, {}", projectId, userId);
		RestAuthorizationRole role = restProjectService.getProjectAuthorizationRole(projectId, userId);
		return mapper.writeValueAsString(role);
	}

	@Path("/projects/{project-id}/members/{userId}/role")
	@PUT
	public void addProjectMemberAuthorizationRole(@PathParam("project-id") String projectId,
	                                            @PathParam("userId") String userId, String roleJson)
		throws EngineException
	{
		log.info("addProjectMemberAuthorizationRole {}, {}", projectId, userId);
		RestAuthorizationRole role = parse(roleJson, RestAuthorizationRole.class);
		restProjectService.setProjectAuthorizationRole(
			projectId, userId, role);
	}

	public <T> T parse(String contents, Class<T> clazz)
	{
		try
		{
			return mapper.readValue(contents, clazz);
		}
		catch (Exception e)
		{
			if(e.getCause() instanceof BadRequestException)
				throw (BadRequestException)e.getCause();
			throw new BadRequestException("Can't perform JSON deserialization", e);
		}
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
		private final EntityManagement idsMan;

		@Autowired
		RESTUpmanControllerFactory(ObjectFactory<RESTUpmanController> factory,
		                           @Qualifier("insecure") DelegatedGroupManagement delGroupMan,
		                           @Qualifier("insecure") GroupsManagement groupMan,
		                           @Qualifier("insecure") GroupDelegationConfigGenerator groupDelegationConfigGenerator,
		                           UpmanRestAuthorizationManager authz,
		                           @Qualifier("insecure") RegistrationsManagement registrationsManagement,
		                           @Qualifier("insecure") EnquiryManagement enquiryManagement,
		                           @Qualifier("insecure") EntityManagement idsMan)
		{
			this.factory = factory;
			this.delGroupMan = delGroupMan;
			this.groupMan = groupMan;
			this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
			this.authz = authz;
			this.registrationsManagement = registrationsManagement;
			this.enquiryManagement = enquiryManagement;
			this.idsMan = idsMan;
		}

		public RESTUpmanController newInstance(String rootGroup, String authorizeGroup)
		{
			RESTUpmanController object = factory.getObject();
			object.init(new RestProjectService(
				delGroupMan, groupMan, groupDelegationConfigGenerator, registrationsManagement, enquiryManagement,
				authz, idsMan, rootGroup, authorizeGroup
			), rootGroup);
			return object;
		}
	}

}




