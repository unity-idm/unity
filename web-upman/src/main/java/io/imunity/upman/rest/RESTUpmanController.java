/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.api.types.policy.RestPolicyDocumentUpdateRequest;
import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.upman.rest.RestProjectFormService.RestProjectFormServiceFactory;
import io.imunity.upman.rest.RestProjectPolicyDocumentService.RestProjectPolicyDocumentServiceFactory;
import io.imunity.upman.rest.RestProjectService.RestProjectServiceFactory;
import io.imunity.upman.rest.RestUpmanWorkflowService.Factory;
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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
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
	private final UpmanJsonProvider jsonProvider = new UpmanJsonProvider();
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
	}

	@Path("/projects")
	@GET
	@Operation(operationId = "getProjects", summary = "List managed projects")
	@ApiResponse(responseCode = "200", description = "Projects visible to the caller",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RestProject.class))))
	public List<RestProject> getProjects() throws EngineException
	{
		log.debug("getProjects query for " + rootGroup);
		return restProjectService.getProjects();
	}

	@Path("/projects/{project-id}")
	@GET
	@Operation(operationId = "getProject", summary = "Read a project")
	@ApiResponse(responseCode = "200", description = "Project configuration",
			content = @Content(schema = @Schema(implementation = RestProject.class)))
	public RestProject getProject(@PathParam("project-id") String projectId) throws EngineException
	{
		log.debug("getProject query for " + projectId);
		return restProjectService.getProject(projectId);
	}

	@Path("/projects")
	@POST
	@Operation(operationId = "addProject", summary = "Create a project")
	@ApiResponse(responseCode = "200", description = "Created project identifier",
			content = @Content(schema = @Schema(implementation = RestProjectId.class)))
	@RequestBody(required = true, content = {
			@Content(mediaType = "application/json", schema = @Schema(implementation = RestProjectCreateRequest.class)),
			@Content(mediaType = "text/plain", schema = @Schema(implementation = RestProjectCreateRequest.class)),
			@Content(mediaType = "application/octet-stream", schema = @Schema(implementation = RestProjectCreateRequest.class))})
	public RestProjectId addProject(RestProjectCreateRequest project) throws EngineException
	{
		log.info("addProject {}", project);
		return restProjectService.addProject(project);
	}

	@Path("/projects/{project-id}")
	@PUT
	@Operation(operationId = "updateProject", summary = "Update a project")
	@ApiResponse(responseCode = "204", description = "Project updated")
	@RequestBody(required = true, content = {
			@Content(mediaType = "application/json", schema = @Schema(implementation = RestProjectUpdateRequest.class)),
			@Content(mediaType = "text/plain", schema = @Schema(implementation = RestProjectUpdateRequest.class)),
			@Content(mediaType = "application/octet-stream", schema = @Schema(implementation = RestProjectUpdateRequest.class))})
	public void updateProject(@PathParam("project-id") String projectId, RestProjectUpdateRequest project)
			throws EngineException
	{
		log.info("updateProject {}", project);
		restProjectService.updateProject(projectId, project);
	}

	@Path("/projects/{project-id}")
	@DELETE
	@Operation(operationId = "removeProject", summary = "Delete a project")
	@ApiResponse(responseCode = "204", description = "Project deleted")
	public void removeProject(@PathParam("project-id") String projectId) throws EngineException
	{
		log.info("removeProject {}", projectId);
		restProjectService.removeProject(projectId);
	}

	@Path("/projects/{project-id}/members")
	@GET
	@Operation(operationId = "getProjectMembers")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(anyOf = {RestProjectMembership[].class, RestGroupMember[].class})))
	public List<? extends RestMember> getProjectMembers(@PathParam("project-id") String projectId,
			@QueryParam("groupPath") String groupPath)
			throws EngineException
	{
		log.debug("getProjectMember {}", projectId);
		return groupPath == null ? restProjectService.getProjectMembers(projectId)
				: workflows.members(projectId, groupPath);
	}

	@Path("/projects/{project-id}/invitations")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "sendInvitations")
	@ApiResponse(responseCode = "201", description = "Success",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RestInvitationOutcome.class))))
	public Response sendInvitations(@PathParam("project-id") String projectId, RestInvitationRequest input)
			throws EngineException
	{
		return Response.status(Response.Status.CREATED).entity(
				new GenericEntity<List<RestInvitationOutcome>>(workflows.sendInvitations(projectId, input)) {}).build();
	}

	@Path("/projects/{project-id}/invitations")
	@GET
	@Operation(operationId = "getInvitations")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RestInvitation.class))))
	public List<RestInvitation> getInvitations(@PathParam("project-id") String projectId)
			throws EngineException
	{
		return workflows.invitations(projectId);
	}

	@Path("/projects/{project-id}/invitations/{code}")
	@GET
	@Operation(operationId = "getInvitation")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestInvitation.class)))
	public RestInvitation getInvitation(@PathParam("project-id") String projectId, @PathParam("code") String code)
			throws EngineException
	{
		return workflows.invitation(projectId, code);
	}

	@Path("/projects/{project-id}/invitations/{code}")
	@DELETE
	@Operation(operationId = "removeInvitation")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeInvitation(@PathParam("project-id") String projectId, @PathParam("code") String code)
			throws EngineException
	{
		workflows.removeInvitation(projectId, code);
	}

	@Path("/projects/{project-id}/invitations/{code}/resend")
	@POST
	@Operation(operationId = "resendInvitation")
	@ApiResponse(responseCode = "204", description = "Success")
	public void resendInvitation(@PathParam("project-id") String projectId, @PathParam("code") String code)
			throws EngineException
	{
		workflows.resendInvitation(projectId, code);
	}

	@Path("/projects/{project-id}/invitations/{code}/reinvite")
	@POST
	@Operation(operationId = "reinvite")
	@ApiResponse(responseCode = "204", description = "Success")
	public void reinvite(@PathParam("project-id") String projectId, @PathParam("code") String code)
			throws EngineException
	{
		workflows.reinvite(projectId, code);
	}

	@Path("/projects/{project-id}/groups")
	@GET
	@Operation(operationId = "getGroups")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RestGroup.class))))
	public List<RestGroup> getGroups(@PathParam("project-id") String projectId) throws EngineException
	{
		return workflows.groups(projectId);
	}

	@Path("/projects/{project-id}/groups")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addGroup")
	@ApiResponse(responseCode = "201", description = "Success",
			content = @Content(schema = @Schema(implementation = RestGroupPath.class)))
	public Response addGroup(@PathParam("project-id") String projectId, RestGroupCreateRequest input)
			throws EngineException
	{
		return Response.status(Response.Status.CREATED).entity(workflows.addGroup(projectId, input)).build();
	}

	@Path("/projects/{project-id}/groups/by-path")
	@GET
	@Operation(operationId = "getGroup")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestGroup.class)))
	public RestGroup getGroup(@PathParam("project-id") String projectId, @QueryParam("path") String path)
			throws EngineException
	{
		return workflows.group(projectId, path);
	}

	@Path("/projects/{project-id}/groups/by-path")
	@DELETE
	@Operation(operationId = "removeGroup")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeGroup(@PathParam("project-id") String projectId, @QueryParam("path") String path)
			throws EngineException
	{
		workflows.removeGroup(projectId, path);
	}

	@Path("/projects/{project-id}/groups")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateGroup")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateGroup(@PathParam("project-id") String projectId, RestGroupUpdateRequest input)
			throws EngineException
	{
		workflows.updateGroup(projectId, input);
	}

	@Path("/projects/{project-id}/groups/by-path/delegation")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "setGroupDelegation")
	@ApiResponse(responseCode = "204", description = "Success")
	public void setGroupDelegation(@PathParam("project-id") String projectId, @QueryParam("path") String path,
			RestGroupDelegationRequest input) throws EngineException
	{
		workflows.setDelegation(projectId, path, input);
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}")
	@PUT
	@Operation(operationId = "addGroupMember")
	@ApiResponse(responseCode = "204", description = "Success")
	public void addGroupMember(@PathParam("project-id") String projectId, @PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath) throws EngineException
	{
		workflows.addMember(projectId, groupPath, entityId);
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}")
	@GET
	@Operation(operationId = "getGroupMember")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestGroupMember.class)))
	public RestGroupMember getGroupMember(@PathParam("project-id") String projectId, @PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath) throws EngineException
	{
		return workflows.member(projectId, groupPath, entityId);
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}")
	@DELETE
	@Operation(operationId = "removeGroupMember")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeGroupMember(@PathParam("project-id") String projectId, @PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath) throws EngineException
	{
		workflows.removeMember(projectId, groupPath, entityId);
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}/role")
	@GET
	@Operation(operationId = "getGroupMemberRole")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestAuthorizationRole.class)))
	public RestAuthorizationRole getGroupMemberRole(@PathParam("project-id") String projectId,
			@PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath) throws EngineException
	{
		return workflows.role(projectId, groupPath, entityId);
	}

	@Path("/projects/{project-id}/members/by-id/{entityId}/role")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "setGroupMemberRole")
	@ApiResponse(responseCode = "204", description = "Success")
	public void setGroupMemberRole(@PathParam("project-id") String projectId, @PathParam("entityId") long entityId,
			@QueryParam("groupPath") String groupPath, RestAuthorizationRole role) throws EngineException
	{
		workflows.setRole(projectId, groupPath, entityId, role);
	}

	@Path("/projects/{project-id}/requests")
	@GET
	@Operation(operationId = "getRequests")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RestRequest.class))))
	public List<RestRequest> getRequests(@PathParam("project-id") String projectId) throws EngineException
	{
		return workflows.requests(projectId);
	}

	@Path("/projects/{project-id}/requests/{requestId}")
	@GET
	@Operation(operationId = "getRequest")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestRequest.class)))
	public RestRequest getRequest(@PathParam("project-id") String projectId, @PathParam("requestId") String requestId)
			throws EngineException
	{
		return workflows.request(projectId, requestId);
	}

	@Path("/projects/{project-id}/requests/{requestId}/state")
	@POST
	@Operation(operationId = "decideRequest")
	@ApiResponse(responseCode = "204", description = "Success")
	public void decideRequest(@PathParam("project-id") String projectId, @PathParam("requestId") String requestId,
			@QueryParam("decision") String decision)
			throws EngineException
	{
		workflows.decide(projectId, requestId, decision);
	}

	@Path("/projects/{project-id}/members/{userId}")
	@GET
	@Operation(operationId = "getProjectMember")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestProjectMembership.class)))
	public RestProjectMembership getProjectMember(@PathParam("project-id") String projectId,
			@PathParam("userId") String email)
			throws EngineException
	{
		log.debug("getProjectMember {}, {}", projectId, email);
		return restProjectService.getProjectMember(projectId, email);
	}

	@Path("/projects/{project-id}/members/{userId}")
	@DELETE
	@Operation(operationId = "removeProjectMember")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeProjectMember(@PathParam("project-id") String projectId, @PathParam("userId") String userId)
			throws EngineException
	{
		log.info("removeProjectMember {}, {}", projectId, userId);
		restProjectService.removeProjectMember(projectId, userId);
	}

	@Path("/projects/{project-id}/members/{userId}")
	@POST
	@Operation(operationId = "addProjectMember")
	@ApiResponse(responseCode = "204", description = "Success")
	public void addProjectMember(@PathParam("project-id") String projectId, @PathParam("userId") String userId)
			throws EngineException
	{
		log.info("removeProjectMember {}, {}", projectId, userId);
		restProjectService.addProjectMember(projectId, userId);
	}

	@Path("/projects/{project-id}/members/{userId}/role")
	@GET
	@Operation(operationId = "getProjectMemberAuthorizationRole")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestAuthorizationRole.class)))
	public RestAuthorizationRole getProjectMemberAuthorizationRole(@PathParam("project-id") String projectId,
			@PathParam("userId") String userId) throws EngineException
	{
		log.debug("getProjectMemberAuthorizationRole {}, {}", projectId, userId);
		return restProjectService.getProjectAuthorizationRole(projectId, userId);
	}

	@Path("/projects/{project-id}/members/{userId}/role")
	@PUT
	@Operation(operationId = "addProjectMemberAuthorizationRole")
	@ApiResponse(responseCode = "204", description = "Success")
	public void addProjectMemberAuthorizationRole(@PathParam("project-id") String projectId,
			@PathParam("userId") String userId, RestAuthorizationRole role) throws EngineException
	{
		log.info("addProjectMemberAuthorizationRole {}, {}", projectId, userId);
		restProjectService.setProjectAuthorizationRole(projectId, userId, role);
	}

	@Path("/projects/{project-id}/policyDocuments")
	@GET
	@Operation(operationId = "getPolicyDocuments", summary = "List project policy documents")
	@ApiResponse(responseCode = "200", description = "List project policy documents",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RestPolicyDocument.class))))
	public List<RestPolicyDocument> getPolicyDocuments(@PathParam("project-id") String projectId)
			throws EngineException
	{
		log.debug("getPolicyDocuments query for {}", projectId);
		return restProjectPolicyDocumentService.getPolicyDocuments(projectId);
	}

	@Path("/projects/{project-id}/policyDocuments/{policy-id}")
	@GET
	@Operation(operationId = "getPolicyDocument", summary = "Read a project policy document")
	@ApiResponse(responseCode = "200", description = "Read a project policy document",
			content = @Content(schema = @Schema(implementation = RestPolicyDocument.class)))
	public RestPolicyDocument getPolicyDocument(@PathParam("project-id") String projectId,
			@PathParam("policy-id") Long policyId)
			throws EngineException
	{
		log.debug("getPolicyDocument {}, {}", projectId, policyId);
		return restProjectPolicyDocumentService.getPolicyDocument(projectId, policyId);
	}

	@Path("/projects/{project-id}/policyDocuments/{policy-id}")
	@DELETE
	@Operation(operationId = "removePolicyDocument", summary = "Remove a project policy document")
	@ApiResponse(responseCode = "204", description = "Remove a project policy document")
	public void removePolicyDocument(@PathParam("project-id") String projectId, @PathParam("policy-id") Long policyId)
			throws EngineException
	{
		log.debug("removePolicyDocument {}, {}", projectId, policyId);
		restProjectPolicyDocumentService.removePolicyDocument(projectId, policyId);
	}

	@Path("/projects/{project-id}/policyDocuments")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addPolicyDocument", summary = "Create a project policy document")
	@ApiResponse(responseCode = "200", description = "Create a project policy document",
			content = @Content(schema = @Schema(implementation = RestPolicyDocumentId.class)))
	public RestPolicyDocumentId addPolicyDocument(@PathParam("project-id") String projectId,
			RestPolicyDocumentRequest policy)
			throws EngineException
	{
		log.debug("addPolicyDocument {}, {}", projectId, policy.name);
		return restProjectPolicyDocumentService.addPolicyDocument(projectId, policy);
	}

	@Path("/projects/{project-id}/policyDocuments")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updatePolicyDocument", summary = "Update a project policy document")
	@ApiResponse(responseCode = "204", description = "Update a project policy document")
	public void updatePolicyDocument(@PathParam("project-id") String projectId,
			@QueryParam("incrementRevision") Boolean incrementRevision, RestPolicyDocumentUpdateRequest policy)
			throws EngineException
	{
		log.debug("updatePolicyDocument {}, {}", projectId, policy.id);
		restProjectPolicyDocumentService.updatePolicyDocument(projectId, policy,
				incrementRevision != null && incrementRevision);
	}

	@Path("/projects/{project-id}/registrationForm")
	@GET
	@Operation(operationId = "getRegistrationForm")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestRegistrationForm.class)))
	public RestRegistrationForm getRegistrationForm(@PathParam("project-id") String projectId)
			throws EngineException
	{
		log.debug("getRegistrationForm {}", projectId);
		return restProjectFormService.getRegistrationForm(projectId);
	}

	@Path("/projects/{project-id}/registrationForm/link")
	@GET
	@Operation(operationId = "getRegistrationFormLink")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestFormLink.class)))
	public RestFormLink getRegistrationFormLink(@PathParam("project-id") String projectId)
			throws EngineException
	{
		return workflows.registrationFormLink(projectId);
	}

	@Path("/projects/{project-id}/registrationForm")
	@DELETE
	@Operation(operationId = "removeRegistrationForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeRegistrationForm(@PathParam("project-id") String projectId,
			@QueryParam("dropRequests") Boolean dropRequests) throws EngineException
	{
		log.debug("removeRegistrationForm {}", projectId);
		restProjectFormService.removeRegistrationForm(projectId, dropRequests != null && dropRequests);
	}

	@Path("/projects/{project-id}/registrationForm")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addRegistrationForm")
	@ApiResponse(responseCode = "204", description = "Success")
	@RequestBody(required = false, description = "Required unless autogenerate=true; ignored when generating",
			content = @Content(mediaType = MediaType.APPLICATION_JSON,
					schema = @Schema(implementation = RestRegistrationForm.class)))
	public void addRegistrationForm(@PathParam("project-id") String projectId,
			@QueryParam("autogenerate") Boolean autogenerate, InputStream body, @Context HttpHeaders headers)
			throws EngineException, IOException
	{
		log.debug("addRegistrationForm {}, autogenerate={}", projectId, autogenerate);

		if (autogenerate != null && autogenerate)
		{
			restProjectFormService.generateRegistrationForm(projectId);
		} else
		{
			RestRegistrationForm form = readForm(body, headers, RestRegistrationForm.class);
			restProjectFormService.addRegistrationForm(projectId, form);
		}
	}

	@Path("/projects/{project-id}/registrationForm")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateRegistrationForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateRegistrationForm(@PathParam("project-id") String projectId,
			@QueryParam("ignoreRequests") Boolean ignoreRequests, RestRegistrationForm form) throws EngineException
	{
		log.debug("updateRegistrationForm {}, {}", projectId, form.name);
		restProjectFormService.updateRegistrationForm(projectId, form, ignoreRequests != null && ignoreRequests);
	}

	@Path("/projects/{project-id}/signUpEnquiry")
	@GET
	@Operation(operationId = "getSignupEnquiryForm")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestEnquiryForm.class)))
	public RestEnquiryForm getSignupEnquiryForm(@PathParam("project-id") String projectId)
			throws EngineException
	{
		log.debug("getSignUpEnquiryForm {}", projectId);
		return restProjectFormService.getSignupEnquiryForm(projectId);
	}

	@Path("/projects/{project-id}/signUpEnquiry/link")
	@GET
	@Operation(operationId = "getSignupEnquiryLink")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestFormLink.class)))
	public RestFormLink getSignupEnquiryLink(@PathParam("project-id") String projectId)
			throws EngineException
	{
		return workflows.signUpEnquiryLink(projectId);
	}

	@Path("/projects/{project-id}/signUpEnquiry")
	@DELETE
	@Operation(operationId = "removeSignupEnquiryForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeSignupEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("dropRequests") Boolean dropRequests) throws EngineException
	{
		log.debug("removeSignupEnquiryForm {}", projectId);
		restProjectFormService.removeSignupEnquiryForm(projectId, dropRequests != null && dropRequests);
	}

	@Path("/projects/{project-id}/signUpEnquiry")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addSignupEnquiryForm")
	@ApiResponse(responseCode = "204", description = "Success")
	@RequestBody(required = false, description = "Required unless autogenerate=true; ignored when generating",
			content = @Content(mediaType = MediaType.APPLICATION_JSON,
					schema = @Schema(implementation = RestEnquiryForm.class)))
	public void addSignupEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("autogenerate") Boolean autogenerate, InputStream body, @Context HttpHeaders headers)
			throws EngineException, IOException
	{
		log.debug("addSignupEnquiryForm {}, autogenerate={}", projectId, autogenerate);

		if (autogenerate != null && autogenerate)
		{
			restProjectFormService.generateSignupEnquiryForm(projectId);
		} else
		{
			RestEnquiryForm form = readForm(body, headers, RestEnquiryForm.class);
			restProjectFormService.addSignupEnquiryForm(projectId, form);
		}

	}

	@Path("/projects/{project-id}/signUpEnquiry")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateSignupEnquiryForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateSignupEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("ignoreRequests") Boolean ignoreRequests, RestEnquiryForm form) throws EngineException
	{
		log.debug("updateSignupEnquiryForm {}, {}", projectId, form.name);
		restProjectFormService.updateSignupEnquiryForm(projectId, form, ignoreRequests != null && ignoreRequests);
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry")
	@GET
	@Operation(operationId = "getMembershipUpdateEnquiryForm")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestEnquiryForm.class)))
	public RestEnquiryForm getMembershipUpdateEnquiryForm(@PathParam("project-id") String projectId)
			throws EngineException
	{
		log.debug("getMembershipUpdateEnquiryForm {}", projectId);
		return restProjectFormService.getMembershipUpdateEnquiryForm(projectId);
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry/link")
	@GET
	@Operation(operationId = "getMembershipUpdateEnquiryLink")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(schema = @Schema(implementation = RestFormLink.class)))
	public RestFormLink getMembershipUpdateEnquiryLink(@PathParam("project-id") String projectId)
			throws EngineException
	{
		return workflows.membershipUpdateEnquiryLink(projectId);
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry")
	@DELETE
	@Operation(operationId = "removeMembershipUpdateEnquiryForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeMembershipUpdateEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("dropRequests") Boolean dropRequests) throws EngineException
	{
		log.debug("removeMembershipUpdateEnquiryForm {}", projectId);
		restProjectFormService.removeMembershipUpdateEnquiryForm(projectId, dropRequests != null && dropRequests);
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addMembershipUpdateEnquiryForm")
	@ApiResponse(responseCode = "204", description = "Success")
	@RequestBody(required = false, description = "Required unless autogenerate=true; ignored when generating",
			content = @Content(mediaType = MediaType.APPLICATION_JSON,
					schema = @Schema(implementation = RestEnquiryForm.class)))
	public void addMembershipUpdateEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("autogenerate") Boolean autogenerate, InputStream body, @Context HttpHeaders headers)
			throws EngineException, IOException
	{
		log.debug("addMembershipUpdateEnquiryForm {}, autogenerate={}", projectId, autogenerate);

		if (autogenerate != null && autogenerate)
		{
			restProjectFormService.generateMembershipUpdateEnquiryForm(projectId);
		} else
		{
			RestEnquiryForm form = readForm(body, headers, RestEnquiryForm.class);
			restProjectFormService.addMembershipUpdateEnquiryForm(projectId, form);
		}
	}

	@Path("/projects/{project-id}/membershipUpdateEnquiry")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateMembershipUpdateEnquiryForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateMembershipUpdateEnquiryForm(@PathParam("project-id") String projectId,
			@QueryParam("ignoreRequests") Boolean ignoreRequests, RestEnquiryForm form) throws EngineException
	{
		log.debug("updateMembershipUpdateEnquiryForm {}, {}", projectId, form.name);
		restProjectFormService.updateMembershipUpdateEnquiryForm(projectId, form,
				ignoreRequests != null && ignoreRequests);
	}

	private <T> T readForm(InputStream body, HttpHeaders headers, Class<T> type) throws IOException
	{
		return type.cast(jsonProvider.read(body, type, headers.getMediaType()));
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
