/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.project.ProjectInvitation;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

/**
 * Implementation of {@link ProjectInvitationsManagement}
 * 
 * @author P.Piernik
 *
 */

@Component
@Primary
public class ProjectInvitationsManagementImpl implements ProjectInvitationsManagement
{
	private ProjectAuthorizationManager authz;
	private InvitationManagement invitationMan;
	private GroupsManagement groupMan;
	private SharedEndpointManagement sharedEndpointMan;
	private RegistrationsManagement registrationMan;

	public ProjectInvitationsManagementImpl(@Qualifier("insecure") InvitationManagement invitationMan,
			@Qualifier("insecure") GroupsManagement groupMan,
			@Qualifier("insecure") RegistrationsManagement registrationMan,
			SharedEndpointManagement sharedEndpointMan, ProjectAuthorizationManager authz)
	{
		this.invitationMan = invitationMan;
		this.groupMan = groupMan;
		this.sharedEndpointMan = sharedEndpointMan;
		this.registrationMan = registrationMan;
		this.authz = authz;
	}

	@Override
	public String addInvitation(ProjectInvitationParam param) throws EngineException
	{
		String projectPath = param.getProject();
		authz.checkManagerAuthorization(projectPath);

		InvitationParam invitationParam = new InvitationParam(getRegistrationFormForProject(projectPath),
				param.getExpiration(), param.getContactAddress());

		return invitationMan.addInvitation(invitationParam);

	}

	@Override
	public List<ProjectInvitation> getInvitations(String projectPath) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);

		GroupDelegationConfiguration config = getDelegationConfiguration(projectPath);

		String registrationFormId = config.registrationForm;
		List<ProjectInvitation> ret = new ArrayList<>();
		if (registrationFormId == null || registrationFormId.isEmpty())
		{
			return ret;
		}

		Map<String, RegistrationForm> allFormsMap = registrationMan.getForms().stream()
				.collect(Collectors.toMap(RegistrationForm::getName, Function.identity()));

		for (InvitationWithCode inv : invitationMan.getInvitations())
		{
			if (inv.getFormId().equals(registrationFormId))
			{
				ProjectInvitation pinv = new ProjectInvitation(projectPath, inv);
				pinv.setLink(PublicRegistrationURLSupport.getPublicRegistrationLink(
						allFormsMap.get(inv.getFormId()), inv.getRegistrationCode(),
						sharedEndpointMan));
				ret.add(pinv);
			}
		}

		return ret;
	}

	private GroupDelegationConfiguration getDelegationConfiguration(String projectPath) throws EngineException
	{
		GroupContents contents = groupMan.getContents(projectPath, GroupContents.METADATA);
		return contents.getGroup().getDelegationConfiguration();
	}

	@Override
	public void removeInvitation(String projectPath, String code) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		assertIfIsProjectInvitation(projectPath, code);
		invitationMan.removeInvitation(code);
	}

	private String getRegistrationFormForProject(String projectPath) throws EngineException
	{
		GroupDelegationConfiguration config = getDelegationConfiguration(projectPath);
		return config.registrationForm;
	}

	@Override
	public void sendInvitation(String projectPath, String code) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		assertIfIsProjectInvitation(projectPath, code);
		invitationMan.sendInvitation(code);

	}

	private void assertIfIsProjectInvitation(String projectPath, String code) throws EngineException
	{
		String registrationForm = getRegistrationFormForProject(projectPath);
		InvitationWithCode invitation = invitationMan.getInvitation(code);
		if (!invitation.getFormId().equals(registrationForm))
		{
			throw new NotProjectInvitation(projectPath, code);
		}
	}

	private static class NotProjectInvitation extends InternalException
	{
		public NotProjectInvitation(String projectPath, String code)
		{
			super("Invitation with code " + code + " is not related with project group " + projectPath);
		}
	}
}
