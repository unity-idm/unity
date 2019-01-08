/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequest;
import pl.edu.icm.unity.engine.api.project.ProjectRequest.RequestOperation;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Implementation of {@link ProjectRequestManagement}
 * 
 * @author P.Piernik
 *
 */
@Component
public class ProjectRequestManagementImpl implements ProjectRequestManagement
{
	private ProjectAuthorizationManager authz;
	private RegistrationsManagement registrationMan;
	private EnquiryManagement enquiryMan;
	private GroupsManagement groupMan;
	private SharedEndpointManagement sharedEndpointMan;
	private EntityManagement idMan;
	private ProjectAttributeHelper projectAttrHelper;

	public ProjectRequestManagementImpl(ProjectAuthorizationManager authz,
			@Qualifier("insecure") RegistrationsManagement registrationMan,
			@Qualifier("insecure") EnquiryManagement enquiryMan,
			@Qualifier("insecure") GroupsManagement groupMan, @Qualifier("insecure") EntityManagement idMan,
			ProjectAttributeHelper projectAttrHelper, SharedEndpointManagement sharedEndpointMan)
	{
		this.authz = authz;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.groupMan = groupMan;
		this.sharedEndpointMan = sharedEndpointMan;
		this.idMan = idMan;
		this.projectAttrHelper = projectAttrHelper;
	}

	@Transactional
	@Override
	public List<ProjectRequest> getRequests(String projectPath) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);

		List<ProjectRequest> allRequests = new ArrayList<>();

		GroupDelegationConfiguration projectDelegationConfig = getProjectDelegationConfig(projectPath);

		allRequests.addAll(getReqistrationRequests(projectPath, projectDelegationConfig.registrationForm));

		allRequests.addAll(getEnquiryRequests(projectPath, projectDelegationConfig.signupEnquiryForm,
				projectDelegationConfig.stickyEnquiryForm));

		return allRequests;
	}

	@Transactional
	@Override
	public void accept(String projectPath, String id, RequestOperation operation) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);

		if (operation.equals(RequestOperation.SelfSignUp))
			proccessRegistationRequest(projectPath, id, RegistrationRequestAction.accept);
		else if (operation.equals(RequestOperation.Update))
			proccessEnquiryResponse(projectPath, id, RegistrationRequestAction.accept);
	}

	@Transactional
	@Override
	public void decline(String projectPath, String id, RequestOperation operation) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);

		if (operation.equals(RequestOperation.SelfSignUp))
			proccessRegistationRequest(projectPath, id, RegistrationRequestAction.reject);
		else if (operation.equals(RequestOperation.Update))
			proccessEnquiryResponse(projectPath, id, RegistrationRequestAction.reject);

	}

	@Transactional
	@Override
	public Optional<String> getProjectRegistrationFormLink(String projectPath) throws EngineException
	{

		authz.checkManagerAuthorization(projectPath);
		String registrationFormId = getProjectDelegationConfig(projectPath).registrationForm;

		if (registrationFormId == null)
			return Optional.empty();

		RegistrationForm registrationForm = registrationMan.getForms().stream()
				.collect(Collectors.toMap(RegistrationForm::getName, Function.identity()))
				.get(registrationFormId);
		
		if(registrationForm.isByInvitationOnly())
			return Optional.empty();
		
		
		return Optional.ofNullable(PublicRegistrationURLSupport.getPublicRegistrationLink(registrationForm,
				sharedEndpointMan));
	}

	@Transactional
	@Override
	public Optional<String> getProjectEnquiryFormLink(String projectPath) throws EngineException
	{

		authz.checkManagerAuthorization(projectPath);
		String enquiryFormId = getProjectDelegationConfig(projectPath).signupEnquiryForm;
		if (enquiryFormId == null)
			return Optional.empty();
		
		return Optional.ofNullable(
				PublicRegistrationURLSupport.getWellknownEnquiryLink(enquiryFormId, sharedEndpointMan));
	}

	private List<ProjectRequest> getEnquiryRequests(String projectPath, String enquiryId, String stickyEnquiryId)
			throws EngineException
	{
		List<ProjectRequest> requests = new ArrayList<>();
		if (enquiryId == null && stickyEnquiryId == null)
			return requests;

		List<EnquiryResponseState> enquires = enquiryMan.getEnquiryResponses();

		for (EnquiryResponseState state : enquires.stream()
				.filter(e -> e.getStatus().equals(RegistrationRequestStatus.pending))
				.collect(Collectors.toList()))
		{
			EnquiryResponse request = state.getRequest();
			if ((enquiryId != null && request.getFormId().equals(enquiryId))
					|| (stickyEnquiryId != null && request.getFormId().equals(stickyEnquiryId)))
				requests.add(mapToProjectRequest(projectPath, state, request));
		}
		return requests;
	}

	private ProjectRequest mapToProjectRequest(String projectPath, EnquiryResponseState enquiryResponseState,
			EnquiryResponse enquiryResponse) throws EngineException
	{
		long entityId = enquiryResponseState.getEntityId();

		String name = projectAttrHelper.getAttributeFromMeta(entityId, "/",
				EntityNameMetadataProvider.NAME);

		String email = null;
		Entity entity = idMan.getEntity(new EntityParam(entityId));
		if (entity != null)
		{
			List<Identity> identities = entity.getIdentities();
			email = getEmailIdentity(
					identities.stream().map(i -> new IdentityParam(i.getTypeId(), i.getValue()))
							.collect(Collectors.toList()));
		}

		if (email == null)
		{
			email = projectAttrHelper.getAttributeFromMeta(entityId, "/",
					ContactEmailMetadataProvider.NAME);
		}

		return mapToProjectRequest(projectPath, enquiryResponseState, enquiryResponse, name, email,
				RequestOperation.Update);
	}

	private List<ProjectRequest> getReqistrationRequests(String projectPath, String registrationForm)
			throws EngineException
	{
		List<ProjectRequest> requests = new ArrayList<>();
		if (registrationForm == null)
			return requests;

		List<RegistrationRequestState> registrationRequests = registrationMan.getRegistrationRequests();
		for (RegistrationRequestState state : registrationRequests.stream()
				.filter(s -> s.getStatus().equals(RegistrationRequestStatus.pending))
				.collect(Collectors.toList()))
		{
			RegistrationRequest request = state.getRequest();
			if (request.getFormId().equals(registrationForm))
				requests.add(mapToProjectRequest(projectPath, state, request));
		}
		return requests;
	}

	private ProjectRequest mapToProjectRequest(String projectPath,
			RegistrationRequestState registrationRequestState, RegistrationRequest registrationRequest)
			throws EngineException
	{

		String name = projectAttrHelper.searchAttributeValueByMeta(EntityNameMetadataProvider.NAME,
				registrationRequest.getAttributes());
		String email = getEmailIdentity(registrationRequest.getIdentities());
		if (email == null)
			email = projectAttrHelper.searchAttributeValueByMeta(ContactEmailMetadataProvider.NAME,
					registrationRequest.getAttributes());

		return mapToProjectRequest(projectPath, registrationRequestState, registrationRequest, name, email,
				RequestOperation.SelfSignUp);
	}

	private ProjectRequest mapToProjectRequest(String projectPath, UserRequestState<?> state,
			BaseRegistrationInput request, String name, String email, RequestOperation operation)
	{

		return new ProjectRequest(state.getRequestId(), operation, projectPath, name, email,
				(request.getGroupSelections() != null && request.getGroupSelections().get(0) != null)
						? request.getGroupSelections().get(0).getSelectedGroups()
						: null,
				state.getTimestamp().toInstant());
	}

	private String getEmailIdentity(List<IdentityParam> identities)
	{
		for (IdentityParam id : identities)
		{
			if (id != null && id.getTypeId().equals(EmailIdentity.ID))
				return id.getValue();
		}
		return null;
	}

	private GroupDelegationConfiguration getProjectDelegationConfig(String projectPath) throws EngineException
	{
		GroupContents groupCon = groupMan.getContents(projectPath, GroupContents.METADATA);
		return groupCon.getGroup().getDelegationConfiguration();
	}

	private void proccessRegistationRequest(String projectPath, String id, RegistrationRequestAction action)
			throws EngineException
	{
		RegistrationRequestState registrationRequest = registrationMan.getRegistrationRequest(id);
		if (registrationRequest != null)
		{
			registrationMan.processRegistrationRequest(registrationRequest.getRequestId(),
					registrationRequest.getRequest(), action, null, null);
		}
	}

	private void proccessEnquiryResponse(String projectPath, String id, RegistrationRequestAction action)
			throws EngineException
	{
		EnquiryResponseState enquiryResponse = enquiryMan.getEnquiryResponse(id);
		if (enquiryResponse != null)
		{
			enquiryMan.processEnquiryResponse(enquiryResponse.getRequestId(), enquiryResponse.getRequest(),
					action, null, null);
		}
	}
}
