/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequest;
import pl.edu.icm.unity.engine.api.project.ProjectRequest.RequestOperation;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
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
	private AttributesHelper attrHelper;
	private SharedEndpointManagement sharedEndpointMan;
	private EntityManagement idMan;
	private AttributeTypeHelper atHelper;

	public ProjectRequestManagementImpl(ProjectAuthorizationManager authz,
			@Qualifier("insecure") RegistrationsManagement registrationMan,
			@Qualifier("insecure") EnquiryManagement enquiryMan,
			@Qualifier("insecure") GroupsManagement groupMan, @Qualifier("insecure") EntityManagement idMan,
			AttributesHelper attrHelper, AttributeTypeHelper atHelper,
			SharedEndpointManagement sharedEndpointMan)
	{
		this.authz = authz;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.groupMan = groupMan;
		this.attrHelper = attrHelper;
		this.sharedEndpointMan = sharedEndpointMan;
		this.idMan = idMan;
		this.atHelper = atHelper;
	}

	@Transactional
	@Override
	public List<ProjectRequest> getRequests(String projectPath) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);

		List<ProjectRequest> allRequests = new ArrayList<>();

		AttributeType nameAttrType = attrHelper
				.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		String nameAttrName = nameAttrType != null ? nameAttrType.getName() : null;

		AttributeType emailAttrType = attrHelper
				.getAttributeTypeWithSingeltonMetadata(ContactEmailMetadataProvider.NAME);
		String emailAttrName = emailAttrType != null ? emailAttrType.getName() : null;

		GroupDelegationConfiguration projectDelegationConfig = getProjectDelegationConfig(projectPath);

		allRequests.addAll(getReqistrationRequests(projectPath, projectDelegationConfig.registrationForm,
				nameAttrName, emailAttrName));

		allRequests.addAll(getEnquiryRequests(projectPath, projectDelegationConfig.signupEnquiryForm,
				projectDelegationConfig.stickyEnquiryForm, nameAttrName, emailAttrName));

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

		return Optional.ofNullable(PublicRegistrationURLSupport.getPublicRegistrationLink(registrationForm,
				sharedEndpointMan));
	}
	
	@Transactional
	@Override
	public Optional<String> getProjectEnquiryFormLink(String projectPath) throws EngineException
	{

		authz.checkManagerAuthorization(projectPath);
		String enquiryFormId = getProjectDelegationConfig(projectPath).signupEnquiryForm;

		return Optional.ofNullable(PublicRegistrationURLSupport.getWellknownEnquiryLink(enquiryFormId,
				sharedEndpointMan));
	}

	private List<ProjectRequest> getEnquiryRequests(String projectPath, String enquiryId, String stickyEnquiryId,
			String nameAttrName, String emailAttrName) throws EngineException
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
				requests.add(mapToProjectRequest(projectPath, state, request, nameAttrName,
						emailAttrName));
		}
		return requests;
	}

	private ProjectRequest mapToProjectRequest(String projectPath, EnquiryResponseState enquiryResponseState,
			EnquiryResponse enquiryResponse, String nameAttrName, String emailAttrName)
			throws EngineException
	{
		long entityId = enquiryResponseState.getEntityId();

		Map<String, Map<String, AttributeExt>> nameAttributes = attrHelper.getAllAttributesAsMap(entityId,
				projectPath, true, nameAttrName);
		List<String> values = nameAttributes.isEmpty() ? null
				: nameAttributes.get(projectPath).get(nameAttrName).getValues();
		String name = (values != null && !values.isEmpty()) ? values.iterator().next() : null;

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
			Map<String, Map<String, AttributeExt>> emailAttributes = attrHelper
					.getAllAttributesAsMap(entityId, projectPath, true, email);
			Map<String, AttributeExt> emailAttrs = emailAttributes.isEmpty() ? null
					: emailAttributes.get(projectPath);
			email = getAttribute(emailAttrName, emailAttrs.values().stream().map(a -> {
				return (Attribute) a;
			}).collect(Collectors.toList()));
		}

		return mapToProjectRequest(projectPath, enquiryResponseState, enquiryResponse, name, email,
				RequestOperation.Update);
	}

	private List<ProjectRequest> getReqistrationRequests(String projectPath, String registrationForm,
			String nameAttrName, String emailAttrName) throws EngineException
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
				requests.add(mapToProjectRequest(projectPath, state, request, nameAttrName,
						emailAttrName));
		}
		return requests;
	}

	private ProjectRequest mapToProjectRequest(String projectPath,
			RegistrationRequestState registrationRequestState, RegistrationRequest registrationRequest,
			String nameAttrName, String emailAttrName) throws EngineException
	{

		String name = getAttribute(nameAttrName, registrationRequest.getAttributes());
		String email = getEmailIdentity(registrationRequest.getIdentities());
		if (email == null)
			email = getAttribute(emailAttrName, registrationRequest.getAttributes());

		return mapToProjectRequest(projectPath, registrationRequestState, registrationRequest, name, email,
				RequestOperation.SelfSignUp);
	}

	
	private ProjectRequest mapToProjectRequest(String projectPath, UserRequestState<?> state,
			BaseRegistrationInput request, String name, String email, RequestOperation operation)
	{

		return new ProjectRequest(state.getRequestId(), operation, projectPath, name, email,
				request.getGroupSelections().get(0).getSelectedGroups(),
				state.getTimestamp().toInstant());
	}
	
	private String getAttribute(String name, Collection<Attribute> list)
	{
		if (name == null)
			return null;

		for (Attribute attr : list)
		{
			if (attr.getName().equals(name) && attr.getValues() != null && !attr.getValues().isEmpty())
			{
				return getEmailValue(atHelper.getUnconfiguredSyntaxForAttributeName(attr.getName()),
						attr.getValues());
			}
		}
		return null;
	}

	private String getEmailValue(AttributeValueSyntax<?> syntax, Collection<String> values)
	{

		if (values != null && !values.isEmpty())
		{
			String value = values.iterator().next();
			if (syntax != null && syntax.isEmailVerifiable())
			{
				VerifiableEmail email = (VerifiableEmail) syntax.convertFromString(value);
				return email.getValue();
			}
			return value;
		} else
		{
			return null;
		}
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
