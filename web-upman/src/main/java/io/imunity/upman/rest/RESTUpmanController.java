/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import java.io.IOException;
import java.util.List;

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
import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.upman.rest.RestProjectFormService.RestProjectFormServiceFactory;
import io.imunity.upman.rest.RestProjectPolicyDocumentService.RestProjectPolicyDocumentServiceFactory;
import io.imunity.upman.rest.RestProjectService.RestProjectServiceFactory;
import io.imunity.upman.rest.RestUpmanWorkflowService.Factory;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@Produces(MediaType.APPLICATION_JSON)
@Path(RESTUpmanEndpoint.V1_PATH)
@PrototypeComponent
public class RESTUpmanController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTUpmanController.class);
	private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
	private RestProjectService restProjectService;
	private RestProjectPolicyDocumentService restProjectPolicyDocumentService;
	private RestProjectFormService restProjectFormService;
	private RestUpmanWorkflowService workflows;

	private String rootGroup;

	private void init(RestProjectService restProjectService,
			RestProjectPolicyDocumentService restProjectPolicyDocumentService,
			RestProjectFormService restProjectFormService, RestUpmanWorkflowService workflows, String rootGroup)
	{
		this.restProjectService = restProjectService;
		this.restProjectPolicyDocumentService = restProjectPolicyDocumentService;
		this.restProjectFormService = restProjectFormService;
		this.workflows = workflows;
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
	public String getProject(@PathParam("project-id") String projectId) throws EngineException, JsonProcessingException
	{
		log.debug("getProject query for " + projectId);
		RestProject project = restProjectService.getProject(projectId);
		return mapper.writeValueAsString(project);
	}

	@Path("/projects")
	@POST
	public String addProject(String projectJson) throws EngineException, JsonProcessingException
	{
		log.info("addProject {}", projectJson);
		RestProjectCreateRequest project = parse(projectJson, RestProjectCreateRequest.class);
		RestProjectId restProjectId = restProjectService.addProject(project);
		return mapper.writeValueAsString(restProjectId);
	}

	@Path("/projects/{project-id}")
	@PUT
	public void updateProject(@PathParam("project-id") String projectId, String projectJson) throws EngineException
	{
		log.info("updateProject {}", projectJson);
		RestProjectUpdateRequest project = parse(projectJson, RestProjectUpdateRequest.class);
		restProjectService.updateProject(projectId, project);
	}

	@Path("/projects/{project-id}")
	@DELETE
	public void removeProject(@PathParam("project-id") String projectId) throws EngineException
	{
		log.info("removeProject {}", projectId);
		restProjectService.removeProject(projectId);
	}

	@Path("/projects/{project-id}/members")
	@GET
	public String getProjectMembers(@PathParam("project-id") String projectId,
			@QueryParam("groupPath") String groupPath)
			throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMember {}", projectId);
		return mapper.writeValueAsString(groupPath == null ? restProjectService.getProjectMembers(projectId)
				: workflows.members(projectId, groupPath));
	}

	@Path("/projects/{project-id}/invitations")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response sendInvitations(@PathParam("project-id") String projectId, String json)
			throws EngineException, JsonProcessingException
	{
		return Response.status(Response.Status.CREATED).entity(mapper.writeValueAsString(
				workflows.sendInvitations(projectId, parse(json, RestInvitationRequest.class)))).build();
	}

	@Path("/projects/{project-id}/invitations")
	@GET
	public String getInvitations(@PathParam("project-id") String projectId)
			throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.invitations(projectId));
	}

	@Path("/projects/{project-id}/invitations/{code}")
	@GET
	public String getInvitation(@PathParam("project-id") String projectId, @PathParam("code") String code)
			throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.invitation(projectId, code));
	}

	@Path("/projects/{project-id}/invitations/{code}")
	@DELETE
	public void removeInvitation(@PathParam("project-id") String projectId, @PathParam("code") String code)
			throws EngineException
	{
		workflows.removeInvitation(projectId, code);
	}

	@Path("/projects/{project-id}/invitations/{code}/resend")
	@POST
	public void resendInvitation(@PathParam("project-id") String projectId, @PathParam("code") String code)
			throws EngineException
	{
		workflows.resendInvitation(projectId, code);
	}

	@Path("/projects/{project-id}/invitations/{code}/reinvite")
	@POST
	public void reinvite(@PathParam("project-id") String projectId, @PathParam("code") String code)
			throws EngineException
	{
		workflows.reinvite(projectId, code);
	}

	@Path("/projects/{project-id}/groups")
	@GET
	public String getGroups(@PathParam("project-id") String projectId) throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.groups(projectId));
	}

	@Path("/projects/{project-id}/groups")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addGroup(@PathParam("project-id") String projectId, String json)
			throws EngineException, JsonProcessingException
	{
		return Response.status(Response.Status.CREATED).entity(mapper.writeValueAsString(
				workflows.addGroup(projectId, parse(json, RestGroupCreateRequest.class)))).build();
	}

	@Path("/projects/{project-id}/groups/by-path")
	@GET
	public String getGroup(@PathParam("project-id") String projectId, @QueryParam("path") String path)
			throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.group(projectId, path));
	}

	@Path("/projects/{project-id}/groups/by-path")
	@DELETE
	public void removeGroup(@PathParam("project-id") String projectId, @QueryParam("path") String path)
			throws EngineException
	{
		workflows.removeGroup(projectId, path);
	}

	@Path("/projects/{project-id}/groups")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateGroup(@PathParam("project-id") String projectId, String json)
			throws EngineException
	{
		workflows.updateGroup(projectId, parse(json, RestGroupUpdateRequest.class));
	}

	@Path("/projects/{project-id}/groups/by-path/delegation")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setGroupDelegation(@PathParam("project-id") String projectId, @QueryParam("path") String path,
			String json) throws EngineException
	{
		workflows.setDelegation(projectId, path, parse(json, RestGroupDelegationRequest.class));
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}")
	@PUT
	public void addGroupMember(@PathParam("project-id") String projectId, @PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath) throws EngineException
	{
		workflows.addMember(projectId, groupPath, entityId);
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}")
	@GET
	public String getGroupMember(@PathParam("project-id") String projectId, @PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath) throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.member(projectId, groupPath, entityId));
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}")
	@DELETE
	public void removeGroupMember(@PathParam("project-id") String projectId, @PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath) throws EngineException
	{
		workflows.removeMember(projectId, groupPath, entityId);
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}/role")
	@GET
	public String getGroupMemberRole(@PathParam("project-id") String projectId, @PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath) throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.role(projectId, groupPath, entityId));
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}/role")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setGroupMemberRole(@PathParam("project-id") String projectId, @PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath, String json) throws EngineException
	{
		workflows.setRole(projectId, groupPath, entityId, parse(json, RestAuthorizationRole.class));
	}

	@Path("/projects/{project-id}/requests")
	@GET
	public String getRequests(@PathParam("project-id") String projectId) throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.requests(projectId));
	}

	@Path("/projects/{project-id}/requests/{requestId}")
	@GET
	public String getRequest(@PathParam("project-id") String projectId, @PathParam("requestId") String requestId)
			throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.request(projectId, requestId));
	}

	@Path("/projects/{project-id}/requests/{requestId}/state")
	@POST
	public void decideRequest(@PathParam("project-id") String projectId, @PathParam("requestId") String requestId,
			@QueryParam("decision") String decision)
			throws EngineException
	{
		workflows.decide(projectId, requestId, decision);
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
			@PathParam("userId") String userId) throws EngineException, JsonProcessingException
	{
		log.debug("getProjectMemberAuthorizationRole {}, {}", projectId, userId);
		RestAuthorizationRole role = restProjectService.getProjectAuthorizationRole(projectId, userId);
		return mapper.writeValueAsString(role);
	}

	@Path("/projects/{project-id}/members/{userId}/role")
	@PUT
	public void addProjectMemberAuthorizationRole(@PathParam("project-id") String projectId,
			@PathParam("userId") String userId, String roleJson) throws EngineException
	{
		log.info("addProjectMemberAuthorizationRole {}, {}", projectId, userId);
		RestAuthorizationRole role = parse(roleJson, RestAuthorizationRole.class);
		restProjectService.setProjectAuthorizationRole(projectId, userId, role);
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

	@Path("/projects/{project-id}/policyDocuments/{policy-id}")
	@GET
	public String getPolicyDocument(@PathParam("project-id") String projectId, @PathParam("policy-id") Long policyId)
			throws EngineException, JsonProcessingException
	{
		log.debug("getPolicyDocument {}, {}", projectId, policyId);
		RestPolicyDocument policy = restProjectPolicyDocumentService.getPolicyDocument(projectId, policyId);
		return mapper.writeValueAsString(policy);
	}

	@Path("/projects/{project-id}/policyDocuments/{policy-id}")
	@DELETE
	public void removePolicyDocument(@PathParam("project-id") String projectId, @PathParam("policy-id") Long policyId)
			throws EngineException
	{
		log.debug("removePolicyDocument {}, {}", projectId, policyId);
		restProjectPolicyDocumentService.removePolicyDocument(projectId, policyId);
	}

	@Path("/projects/{project-id}/policyDocuments")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public String addPolicyDocument(@PathParam("project-id") String projectId, String json)
			throws EngineException, IOException
	{
		RestPolicyDocumentRequest policy = parse(json, RestPolicyDocumentRequest.class);
		log.debug("addPolicyDocument {}, {}", projectId, policy.name);
		RestPolicyDocumentId addPolicyDocument = restProjectPolicyDocumentService.addPolicyDocument(projectId, policy);
		return mapper.writeValueAsString(addPolicyDocument);
	}

	@Path("/projects/{project-id}/policyDocuments")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updatePolicyDocument(@PathParam("project-id") String projectId,
			@QueryParam("incrementRevision") Boolean incrementRevision, String json) throws EngineException
	{
		RestPolicyDocumentUpdateRequest policy = parse(json, RestPolicyDocumentUpdateRequest.class);
		log.debug("updatePolicyDocument {}, {}", projectId, policy.id);
		restProjectPolicyDocumentService.updatePolicyDocument(projectId, policy,
				incrementRevision != null && incrementRevision);
	}

	@Path("/projects/{project-id}/registrationForm")
	@GET
	public String getRegistrationForm(@PathParam("project-id") String projectId)
			throws EngineException, JsonProcessingException
	{
		log.debug("getRegistrationForm {}", projectId);
		RestRegistrationForm form = restProjectFormService.getRegistrationForm(projectId);
		return mapper.writeValueAsString(form);
	}

	@Path("/projects/{project-id}/registrationForm/link")
	@GET
	public String getRegistrationFormLink(@PathParam("project-id") String projectId)
			throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.registrationFormLink(projectId));
	}

	@Path("/projects/{project-id}/registrationForm")
	@DELETE
	public void removeRegistrationForm(@PathParam("project-id") String projectId,
			@QueryParam("dropRequests") Boolean dropRequests) throws EngineException
	{
		log.debug("removeRegistrationForm {}", projectId);
		restProjectFormService.removeRegistrationForm(projectId, dropRequests != null && dropRequests);
	}

	@Path("/projects/{project-id}/registrationForm")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void addRegistrationForm(@PathParam("project-id") String projectId,
			@QueryParam("autogenerate") Boolean autogenerate, String json) throws EngineException
	{

		log.debug("addRegistrationForm {}, autogenerate={}", projectId, autogenerate);

		if (autogenerate != null && autogenerate)
		{
			restProjectFormService.generateRegistrationForm(projectId);
		} else
		{
			RestRegistrationForm form = parse(json, RestRegistrationForm.class);
			restProjectFormService.addRegistrationForm(projectId, form);
		}
	}

	@Path("/projects/{project-id}/registrationForm")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateRegistrationForm(@PathParam("project-id") String projectId,
			@QueryParam("ignoreRequests") Boolean ignoreRequests, String json) throws EngineException
	{
		RestRegistrationForm form = parse(json, RestRegistrationForm.class);
		log.debug("updateRegistrationForm {}, {}", projectId, form.name);
		restProjectFormService.updateRegistrationForm(projectId, form, ignoreRequests != null && ignoreRequests);
	}

	@Path("/projects/{project-id}/signUpEnquiry")
	@GET
	public String getSignupEnquiryForm(@PathParam("project-id") String projectId)
			throws EngineException, JsonProcessingException
	{
		log.debug("getSignUpEnquiryForm {}", projectId);
		RestEnquiryForm form = restProjectFormService.getSignupEnquiryForm(projectId);
		return mapper.writeValueAsString(form);
	}

	@Path("/projects/{project-id}/signUpEnquiry/link")
	@GET
	public String getSignupEnquiryLink(@PathParam("project-id") String projectId)
			throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.signUpEnquiryLink(projectId));
	}

	@Path("/projects/{project-id}/signUpEnquiry")
	@DELETE
	public void removeSignupEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("dropRequests") Boolean dropRequests) throws EngineException
	{
		log.debug("removeSignupEnquiryForm {}", projectId);
		restProjectFormService.removeSignupEnquiryForm(projectId, dropRequests != null && dropRequests);
	}

	@Path("/projects/{project-id}/signUpEnquiry")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void addSignupEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("autogenerate") Boolean autogenerate, String json) throws EngineException
	{
		log.debug("addSignupEnquiryForm {}, autogenerate={}", projectId, autogenerate);

		if (autogenerate != null && autogenerate)
		{
			restProjectFormService.generateSignupEnquiryForm(projectId);
		} else
		{
			RestEnquiryForm form = parse(json, RestEnquiryForm.class);
			restProjectFormService.addSignupEnquiryForm(projectId, form);
		}

	}

	@Path("/projects/{project-id}/signUpEnquiry")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateSignupEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("ignoreRequests") Boolean ignoreRequests, String json) throws EngineException
	{
		RestEnquiryForm form = parse(json, RestEnquiryForm.class);
		log.debug("updateSignupEnquiryForm {}, {}", projectId, form.name);
		restProjectFormService.updateSignupEnquiryForm(projectId, form, ignoreRequests != null && ignoreRequests);
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry")
	@GET
	public String getMembershipUpdateEnquiryForm(@PathParam("project-id") String projectId)
			throws EngineException, JsonProcessingException
	{
		log.debug("getMembershipUpdateEnquiryForm {}", projectId);
		RestEnquiryForm form = restProjectFormService.getMembershipUpdateEnquiryForm(projectId);
		return mapper.writeValueAsString(form);
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry/link")
	@GET
	public String getMembershipUpdateEnquiryLink(@PathParam("project-id") String projectId)
			throws EngineException, JsonProcessingException
	{
		return mapper.writeValueAsString(workflows.membershipUpdateEnquiryLink(projectId));
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry")
	@DELETE
	public void removeMembershipUpdateEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("dropRequests") Boolean dropRequests) throws EngineException
	{
		log.debug("removeMembershipUpdateEnquiryForm {}", projectId);
		restProjectFormService.removeMembershipUpdateEnquiryForm(projectId, dropRequests != null && dropRequests);
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void addMembershipUpdateEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("autogenerate") Boolean autogenerate, String json) throws EngineException
	{
		log.debug("addMembershipUpdateEnquiryForm {}, autogenerate={}", projectId, autogenerate);

		if (autogenerate != null && autogenerate)
		{
			restProjectFormService.generateMembershipUpdateEnquiryForm(projectId);
		} else
		{
			RestEnquiryForm form = parse(json, RestEnquiryForm.class);
			restProjectFormService.addMembershipUpdateEnquiryForm(projectId, form);
		}
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateMembershipUpdateEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("ignoreRequests") Boolean ignoreRequests, String json) throws EngineException
	{
		RestEnquiryForm form = parse(json, RestEnquiryForm.class);
		log.debug("updateMembershipUpdateEnquiryForm {}, {}", projectId, form.name);
		restProjectFormService.updateMembershipUpdateEnquiryForm(projectId, form,
				ignoreRequests != null && ignoreRequests);
	}

	public <T> T parse(String contents, Class<T> clazz)
	{
		try
		{
			return mapper.readValue(contents, clazz);
		} catch (Exception e)
		{
			if (e.getCause() instanceof BadRequestException)
				throw (BadRequestException) e.getCause();
			throw new BadRequestException("Can't perform JSON deserialization", e);
		}
	}

	@Component
	public static class RESTUpmanControllerFactory
	{
		private final ObjectFactory<RESTUpmanController> factory;
		private final RestProjectServiceFactory restProjectServiceFactory;
		private final RestProjectPolicyDocumentServiceFactory restProjectPolicyDocumentServiceFactory;
		private final RestProjectFormServiceFactory restProjectFormServiceFactory;
		private final Factory workflowFactory;

		@Autowired
		RESTUpmanControllerFactory(ObjectFactory<RESTUpmanController> factory,
				RestProjectServiceFactory restProjectServiceFactory,
				RestProjectPolicyDocumentServiceFactory restProjectPolicyDocumentServiceFactory,
				RestProjectFormServiceFactory restFormServiceFactory, Factory workflowFactory)

		{
			this.factory = factory;
			this.restProjectServiceFactory = restProjectServiceFactory;
			this.restProjectPolicyDocumentServiceFactory = restProjectPolicyDocumentServiceFactory;
			this.restProjectFormServiceFactory = restFormServiceFactory;
			this.workflowFactory = workflowFactory;
		}

		public RESTUpmanController newInstance(String rootGroup, String authorizeGroup,
				List<String> rootGroupAttributes)
		{
			RESTUpmanController object = factory.getObject();
			object.init(restProjectServiceFactory.newInstance(rootGroup, authorizeGroup),
					restProjectPolicyDocumentServiceFactory.newInstance(rootGroup, authorizeGroup),
					restProjectFormServiceFactory.newInstance(rootGroup, authorizeGroup, rootGroupAttributes),
					workflowFactory.newInstance(rootGroup, authorizeGroup),
					rootGroup);
			return object;
		}
	}

}
