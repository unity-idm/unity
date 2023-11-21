/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.api.types.policy.RestPolicyDocumentUpdateRequest;
import io.imunity.upman.rest.RestProjectPolicyDocumentService.RestProjectPolicyDocumentServiceFactory;
import io.imunity.upman.rest.RestProjectService.RestProjectServiceFactory;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;

@Produces(MediaType.APPLICATION_JSON)
@Path(RESTUpmanEndpoint.V1_PATH)
@PrototypeComponent
public class RESTUpmanController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTUpmanController.class);
	private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
	private RestProjectService restProjectService;
	private RestProjectPolicyDocumentService restProjectPolicyDocumentService;

	private String rootGroup;

	private void init(RestProjectService restProjectService, RestProjectPolicyDocumentService restProjectPolicyDocumentService,
			String rootGroup)
	{
		this.restProjectService = restProjectService;
		this.restProjectPolicyDocumentService = restProjectPolicyDocumentService;
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
	
	
	@Path("/projects/{project-id}/policyDocuments")
	@GET
	public String getPolicyDocuments(@PathParam("project-id") String projectId)
			throws EngineException, JsonProcessingException
	{
		log.debug("getPolicyDocuments query for {}", projectId);
		List<RestPolicyDocument> policies = restProjectPolicyDocumentService.getPolicyDocuments(projectId);
		return mapper.writeValueAsString(policies);
	}

	@Path("/projects/{project-id}/policyDocument/{policy-id}")
	@GET
	public String getPolicyDocument(@PathParam("project-id") String projectId, @PathParam("policy-id") Long policyId)
			throws EngineException, JsonProcessingException
	{
		log.debug("getPolicyDocument {}, {}", projectId, policyId);
		RestPolicyDocument policy = restProjectPolicyDocumentService.getPolicyDocument(projectId, policyId);
		return mapper.writeValueAsString(policy);
	}

	@Path("/projects/{project-id}/policyDocument/{policy-id}")
	@DELETE
	public void removePolicyDocument(@PathParam("project-id") String projectId, @PathParam("policy-id") Long policyId)
			throws EngineException
	{
		log.debug("removePolicyDocument {}, {}", projectId, policyId);
		restProjectPolicyDocumentService.removePolicyDocument(projectId, policyId);
	}

	@Path("/projects/{project-id}/policyDocument")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void addPolicyDocument(@PathParam("project-id") String projectId, String json)
			throws EngineException, IOException
	{
		RestPolicyDocumentRequest policy = JsonUtil.parse(json, RestPolicyDocumentRequest.class);
		log.debug("addPolicyDocument {}, {}", projectId, policy.name);
		restProjectPolicyDocumentService.addPolicyDocument(projectId, policy);
	}

	@Path("/projects/{project-id}/policyDocument")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updatePolicyDocument(@PathParam("project-id") String projectId, String json)
			throws EngineException, IOException
	{
		RestPolicyDocumentUpdateRequest policy = JsonUtil.parse(json, RestPolicyDocumentUpdateRequest.class);
		log.debug("updatePolicyDocument {}, {}", projectId, policy.id);
		restProjectPolicyDocumentService.updatePolicyDocument(projectId, policy);
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
		private final RestProjectServiceFactory restProjectServiceFactory;
		private final RestProjectPolicyDocumentServiceFactory restProjectPolicyDocumentServiceFactory;

		@Autowired
		RESTUpmanControllerFactory(ObjectFactory<RESTUpmanController> factory,
				RestProjectServiceFactory restProjectServiceFactory,
				RestProjectPolicyDocumentServiceFactory restProjectPolicyDocumentServiceFactory)

		{
			this.factory = factory;
			this.restProjectServiceFactory = restProjectServiceFactory;
			this.restProjectPolicyDocumentServiceFactory = restProjectPolicyDocumentServiceFactory;
		}

		public RESTUpmanController newInstance(String rootGroup, String authorizeGroup)
		{
			RESTUpmanController object = factory.getObject();
			object.init(restProjectServiceFactory.newInstance(rootGroup, authorizeGroup),
					restProjectPolicyDocumentServiceFactory.newInstance(rootGroup, authorizeGroup), rootGroup);
			return object;
		}
	}

}




