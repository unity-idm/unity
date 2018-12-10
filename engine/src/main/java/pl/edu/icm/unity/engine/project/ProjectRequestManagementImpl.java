/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequest;
import pl.edu.icm.unity.engine.api.project.ProjectRequest.RequestOperation;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 *  Implementation of {@link ProjectRequestManagement}
 * @author P.Piernik
 *
 */
@Component
public class ProjectRequestManagementImpl implements ProjectRequestManagement
{
	private ProjectAuthorizationManager authz;
	private RegistrationsManagement registrationMan;
	private GroupsManagement groupMan;
	private AttributesHelper attrHelper;

	public ProjectRequestManagementImpl(ProjectAuthorizationManager authz,
			@Qualifier("insecure") RegistrationsManagement registrationMan,
			@Qualifier("insecure") GroupsManagement groupMan, AttributesHelper attrHelper)
	{
		this.authz = authz;
		this.registrationMan = registrationMan;
		this.groupMan = groupMan;
		this.attrHelper = attrHelper;
	}

	@Transactional
	@Override
	public List<ProjectRequest> getRequests(String projectPath) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);

		List<ProjectRequest> allRequests = new ArrayList<>();

		allRequests.addAll(getReqistrationRequest(projectPath,
				getProjectDelegationConfig(projectPath).registrationForm));

		return allRequests;
	}

	@Transactional
	@Override
	public void accept(String projectPath, String id, RequestOperation operation) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);

		if (operation.equals(RequestOperation.SelfSignUp))
			proccessRegistationRequest(projectPath, id, RegistrationRequestAction.accept);
	}

	@Transactional
	@Override
	public void decline(String projectPath, String id, RequestOperation operation) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);

		if (operation.equals(RequestOperation.SelfSignUp))
			proccessRegistationRequest(projectPath, id, RegistrationRequestAction.reject);

	}

	private List<ProjectRequest> getReqistrationRequest(String projectPath, String registrationForm)
			throws EngineException
	{
		List<ProjectRequest> requests = new ArrayList<>();
		List<RegistrationRequestState> registrationRequests = registrationMan.getRegistrationRequests();
		for (RegistrationRequestState state : registrationRequests.stream()
				.filter(s -> s.getStatus().equals(RegistrationRequestStatus.pending)).collect(Collectors.toList()))
		{
			RegistrationRequest request = state.getRequest();
			if (request.getFormId().equals(registrationForm))
				requests.add(mapToProjectRequest(projectPath, state, request));
		}
		return requests;
	}

	private String getAttribute(String name, List<Attribute> list)
	{
		for (Attribute attr : list)
		{
			if (attr.getName().equals(name) && attr.getValues() != null && !attr.getValues().isEmpty())
			{
				return attr.getValues().get(0);
			}
		}
		return null;
	}

	private ProjectRequest mapToProjectRequest(String projectPath,
			RegistrationRequestState registrationRequestState, RegistrationRequest registrationRequest)
			throws EngineException
	{

		AttributeType nameAttrType = attrHelper
				.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		AttributeType emailAttrType = attrHelper
				.getAttributeTypeWithSingeltonMetadata(ContactEmailMetadataProvider.NAME);

		String name = getAttribute(nameAttrType.getName(), registrationRequest.getAttributes());
		String email = getEmailIdentity(registrationRequest.getIdentities());
		if (email == null)
			email = getAttribute(emailAttrType.getName(), registrationRequest.getAttributes());

		return new ProjectRequest(registrationRequestState.getRequestId(), RequestOperation.SelfSignUp,
				projectPath, name, email,
				registrationRequest.getGroupSelections().get(0).getSelectedGroups(),
				registrationRequestState.getTimestamp().toInstant());
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
}
