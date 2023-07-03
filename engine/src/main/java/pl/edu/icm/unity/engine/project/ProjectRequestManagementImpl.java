/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.BaseRegistrationInput;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequest;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestOperation;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.registration.RequestType;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.tx.Transactional;

/**
 * Implementation of {@link ProjectRequestManagement}
 * 
 * @author P.Piernik
 *
 */
@Component
public class ProjectRequestManagementImpl implements ProjectRequestManagement
{
	private final ProjectAuthorizationManager authz;
	private final RegistrationsManagement registrationMan;
	private final EnquiryManagement enquiryMan;
	private final GroupsManagement groupMan;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final EntityManagement idMan;
	private final ProjectAttributeHelper projectAttrHelper;
	private final AttributesHelper attributesHelper;
	
	public ProjectRequestManagementImpl(ProjectAuthorizationManager authz,
			@Qualifier("insecure") RegistrationsManagement registrationMan,
			@Qualifier("insecure") EnquiryManagement enquiryMan,
			@Qualifier("insecure") GroupsManagement groupMan, @Qualifier("insecure") EntityManagement idMan,
			ProjectAttributeHelper projectAttrHelper,
			AttributesHelper attributesHelper,
			PublicRegistrationURLSupport publicRegistrationURLSupport)
	{
		this.authz = authz;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.groupMan = groupMan;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
		this.idMan = idMan;
		this.projectAttrHelper = projectAttrHelper;
		this.attributesHelper = attributesHelper;
	}

	@Transactional
	@Override
	public List<ProjectRequest> getRequests(String projectPath) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath);

		List<ProjectRequest> allRequests = new ArrayList<>();

		GroupDelegationConfiguration projectDelegationConfig = getProjectDelegationConfig(projectPath);

		allRequests.addAll(getReqistrationRequests(projectPath, projectDelegationConfig.registrationForm));

		allRequests.addAll(getEnquiryRequests(projectPath, projectDelegationConfig.signupEnquiryForm,
				RequestOperation.SignUp, RequestType.Enquiry));
		allRequests.addAll(getEnquiryRequests(projectPath, projectDelegationConfig.membershipUpdateEnquiryForm,
				RequestOperation.Update, RequestType.Enquiry));

		return allRequests;
	}

	@Transactional
	@Override
	public void accept(ProjectRequestParam request) throws EngineException
	{
		authz.assertManagerAuthorization(request.project);
		proccessRequest(request, RegistrationRequestAction.accept);
	}

	@Transactional
	@Override
	public void decline(ProjectRequestParam request) throws EngineException
	{
		authz.assertManagerAuthorization(request.project);
		proccessRequest(request, RegistrationRequestAction.reject);
	}

	@Transactional
	@Override
	public Optional<String> getProjectRegistrationFormLink(String projectPath) throws EngineException
	{

		authz.assertManagerAuthorization(projectPath);
		String registrationFormId = getProjectDelegationConfig(projectPath).registrationForm;

		if (registrationFormId == null)
			return Optional.empty();

		RegistrationForm registrationForm = null;
		try
		{
			registrationForm = registrationMan.getForm(registrationFormId);
		} catch (EngineException e)
		{
			return Optional.empty();
		}

		if (registrationForm.isByInvitationOnly())
			return Optional.empty();

		return Optional.ofNullable(publicRegistrationURLSupport.getPublicRegistrationLink(registrationForm));
	}

	@Transactional
	@Override
	public Optional<String> getProjectSignUpEnquiryFormLink(String projectPath) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath);
		String enquiryFormId = getProjectDelegationConfig(projectPath).signupEnquiryForm;
		return getEnquiryLink(enquiryFormId);
	}
	

	@Transactional
	@Override
	public Optional<String> getProjectUpdateMembershipEnquiryFormLink(String projectPath) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath);
		String enquiryFormId = getProjectDelegationConfig(projectPath).membershipUpdateEnquiryForm;
		return getEnquiryLink(enquiryFormId);
	}
	
	private Optional<String> getEnquiryLink(String formId)
	{
		if (formId == null)
			return Optional.empty();
		
		EnquiryForm enquiryForm = null;
		try
		{
			enquiryForm = enquiryMan.getEnquiry(formId);
		} catch (EngineException e)
		{
			return Optional.empty();
		}
			
		if (enquiryForm.isByInvitationOnly())
			return Optional.empty();

		return Optional.ofNullable(
				publicRegistrationURLSupport.getWellknownEnquiryLink(formId));
	}
	

	private void proccessRequest(ProjectRequestParam request, RegistrationRequestAction action)
			throws EngineException
	{
		validateRequestParam(request);

		if (request.operation.equals(RequestOperation.SignUp))
		{
			if (request.type.equals(RequestType.Registration))
			{
				proccessRegistationRequest(request.project, request.id, action);
			} else
			{
				proccessEnquiryResponse(request.project, request.id, action);
			}
		}

		else if (request.operation.equals(RequestOperation.Update))
		{
			proccessEnquiryResponse(request.project, request.id, action);
		}
	}

	private void validateRequestParam(ProjectRequestParam request)
	{
		if (request.operation == null)
			throw new IllegalArgumentException("Can not process request of unknown operation");
		if (request.type == null)
			throw new IllegalArgumentException("Can not process request of unknown type");
		if (request.id == null || request.id.isEmpty())
			throw new IllegalArgumentException("Can not process request of unknown id");
	}

	private void proccessRegistationRequest(String projectPath, String id, RegistrationRequestAction action)
			throws EngineException
	{
		RegistrationRequestState registrationRequest = registrationMan.getRegistrationRequest(id);

		if (registrationRequest != null)
		{
			registrationMan.processRegistrationRequest(registrationRequest.getRequestId(),
					registrationRequest.getRequest(), action, null, null);
		} else
		{
			throw new IllegalArgumentException("Registration request with id " + id + " does not exists");
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
		} else
		{
			throw new IllegalArgumentException("Enquiry response with id " + id + " does not exists");
		}
	}

	private List<ProjectRequest> getEnquiryRequests(String projectPath, String enquiryId,
			RequestOperation operation, RequestType type) throws EngineException
	{
		List<ProjectRequest> requests = new ArrayList<>();
		if (enquiryId == null)
			return requests;

		List<EnquiryResponseState> enquires = enquiryMan.getEnquiryResponses();

		for (EnquiryResponseState state : enquires.stream()
				.filter(e -> e.getStatus().equals(RegistrationRequestStatus.pending))
				.collect(Collectors.toList()))
		{
			EnquiryResponse request = state.getRequest();
			if (request == null)
				continue;
			if (request.getFormId().equals(enquiryId))
				requests.add(mapToProjectRequest(projectPath, state, request, operation, type));
		}
		return requests;
	}

	private ProjectRequest mapToProjectRequest(String projectPath, EnquiryResponseState enquiryResponseState,
			EnquiryResponse enquiryResponse, RequestOperation operation, RequestType type)
			throws EngineException
	{
		long entityId = enquiryResponseState.getEntityId();

		String name = projectAttrHelper.getAttributeFromMeta(entityId, "/", EntityNameMetadataProvider.NAME);

		VerifiableElementBase email = null;
		Entity entity = idMan.getEntity(new EntityParam(entityId));
		if (entity != null)
		{
			List<Identity> identities = entity.getIdentities();
			email = getEmailIdentity(
					identities.stream().map(i -> (IdentityParam) i)
							.collect(Collectors.toList()));
		}

		if (email == null)
		{
			email = projectAttrHelper.getVerifiableAttributeFromMeta(entityId, "/",
					ContactEmailMetadataProvider.NAME);
		}

		return mapToProjectRequest(projectPath, enquiryResponseState, enquiryResponse, name, email, operation,
				type);
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
			if (request == null)
				continue;
			if (request.getFormId().equals(registrationForm))
				requests.add(mapToProjectRequest(projectPath, state, request));
		}
		return requests;
	}

	private ProjectRequest mapToProjectRequest(String projectPath,
			RegistrationRequestState registrationRequestState, RegistrationRequest registrationRequest)
			throws EngineException
	{

		String name = attributesHelper.getFirstValueOfAttributeFilteredByMeta(EntityNameMetadataProvider.NAME,
				registrationRequest.getAttributes()).orElse(null);
		VerifiableElementBase email = getEmailIdentity(registrationRequest.getIdentities());
		if (email == null)
			email = attributesHelper.getFirstVerifiableAttributeValueFilteredByMeta(
					ContactEmailMetadataProvider.NAME, registrationRequest.getAttributes()).orElse(null);

		return mapToProjectRequest(projectPath, registrationRequestState, registrationRequest, name, email,
				RequestOperation.SignUp, RequestType.Registration);
	}

	private ProjectRequest mapToProjectRequest(String projectPath, UserRequestState<?> state,
			BaseRegistrationInput request, String name, VerifiableElementBase email,
			RequestOperation operation, RequestType type)
	{

		return new ProjectRequest(state.getRequestId(), operation, type, projectPath, name, email,
				Optional.ofNullable((request.getGroupSelections() != null && !request.getGroupSelections().isEmpty()
						&& request.getGroupSelections().get(0) != null)
								? request.getGroupSelections().get(0)
										.getSelectedGroups()
								: null),
				state.getTimestamp() != null ? state.getTimestamp().toInstant() : null);
	}

	private VerifiableElementBase getEmailIdentity(List<IdentityParam> identities)
	{
		for (IdentityParam id : identities)
		{
			if (id != null && id.getTypeId().equals(EmailIdentity.ID))
				return new VerifiableElementBase(id.getValue(), id.getConfirmationInfo());
		}
		return null;
	}

	private GroupDelegationConfiguration getProjectDelegationConfig(String projectPath) throws EngineException
	{
		GroupContents groupCon = groupMan.getContents(projectPath, GroupContents.METADATA);
		return groupCon.getGroup().getDelegationConfiguration();
	}
}
