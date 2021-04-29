/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.project.ProjectInvitation;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

@Component
@Primary
public class ProjectInvitationsManagementImpl implements ProjectInvitationsManagement
{
	public static final String INVITATION_PROJECT_NAME_PARAM = "upmanProject";
	
	private final ProjectAuthorizationManager authz;
	private final InvitationManagement invitationMan;
	private final GroupsManagement groupMan;
	private final SharedEndpointManagement sharedEndpointMan;
	private final RegistrationsManagement registrationMan;
	private final EnquiryManagement enquiryMan;
	private final EntityManagement entityMan;
	private final ExistingUserFinder existingUserFinder;
	private final MessageSource msg;
	
	public ProjectInvitationsManagementImpl(@Qualifier("insecure") InvitationManagement invitationMan,
			@Qualifier("insecure") GroupsManagement groupMan,
			@Qualifier("insecure") RegistrationsManagement registrationMan,
			@Qualifier("insecure") EnquiryManagement enquiryMan,
			@Qualifier("insecure") EntityManagement entityMan,
			SharedEndpointManagement sharedEndpointMan, 
			ProjectAuthorizationManager authz,
			ExistingUserFinder existingUserFinder,
			MessageSource msg)
	{
		this.invitationMan = invitationMan;
		this.groupMan = groupMan;
		this.entityMan = entityMan;
		this.sharedEndpointMan = sharedEndpointMan;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.authz = authz;
		this.existingUserFinder = existingUserFinder;
		this.msg = msg;
	}

	@Override
	public String addInvitation(ProjectInvitationParam param) throws EngineException
	{
		authz.assertManagerAuthorization(param.project);

		Long entity = existingUserFinder.getEntityIdByContactAddress(param.contactAddress);
		String code = null;
		if (entity == null)
		{
			code = invitationMan.addInvitation(getRegistrationInvitation(param));
		} else
		{
			assertNotMemberAlready(entity, param.project);
			code = invitationMan.addInvitation(getEnquiryInvitation(param, entity));
		}

		invitationMan.sendInvitation(code);
		return code;
	}

	private void assertNotMemberAlready(long entityId, String projectGroup) throws EngineException
	{
		Map<String, GroupMembership> groups = entityMan.getGroups(new EntityParam(entityId));
		if (groups.containsKey(projectGroup))
			throw new AlreadyMemberException();
	}
	
	private EnquiryInvitationParam getEnquiryInvitation(ProjectInvitationParam param, Long entityId)
			throws EngineException
	{
		EnquiryInvitationParam invitationParam = new EnquiryInvitationParam(
				getEnquiryFormForProject(param.project), param.expiration, param.contactAddress);
		invitationParam.getAllowedGroups().put(0, new GroupSelection(param.allowedGroup));
		invitationParam.getMessageParams().put(
				MessageTemplateDefinition.CUSTOM_VAR_PREFIX + INVITATION_PROJECT_NAME_PARAM,
				getProjectDisplayedName(param.project));
		invitationParam.setEntity(entityId);
		return invitationParam;
	}

	private RegistrationInvitationParam getRegistrationInvitation(ProjectInvitationParam param)
			throws EngineException
	{
		RegistrationInvitationParam invitationParam = new RegistrationInvitationParam(
				getRegistrationFormForProject(param.project), param.expiration, param.contactAddress);
		invitationParam.getAllowedGroups().put(0, new GroupSelection(param.allowedGroup));
		invitationParam.getMessageParams().put(
				MessageTemplateDefinition.CUSTOM_VAR_PREFIX + INVITATION_PROJECT_NAME_PARAM,
				getProjectDisplayedName(param.project));

		IdentityParam emailId = new IdentityParam(EmailIdentity.ID, param.contactAddress);
		emailId.setConfirmationInfo(new ConfirmationInfo(true));
		invitationParam.getIdentities().put(0, new PrefilledEntry<>(emailId, PrefilledEntryMode.HIDDEN));
		return invitationParam;
	}

	@Override
	public List<ProjectInvitation> getInvitations(String projectPath) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath);

		GroupDelegationConfiguration config = getDelegationConfiguration(projectPath);

		String registrationFormId = config.registrationForm;
		String enquiryFormId = config.signupEnquiryForm;

		RegistrationForm registrationForm = (registrationFormId == null || registrationFormId.isEmpty()) ? null
				: registrationMan.getForms().stream()
						.filter(f -> f.getName().equals(registrationFormId)).findAny()
						.orElse(null);

		EnquiryForm enquiryForm = (enquiryFormId == null || enquiryFormId.isEmpty()) ? null
				: enquiryMan.getEnquires().stream().filter(f -> f.getName().equals(enquiryFormId))
						.findAny().orElse(null);

		if (registrationForm == null && enquiryForm == null)
		{
			return Collections.emptyList();
		}

		List<InvitationWithCode> allInv = invitationMan.getInvitations();
		List<ProjectInvitation> ret = new ArrayList<>();
		filterInvitations(allInv, registrationForm, InvitationType.REGISTRATION).stream().forEach(
				i -> ret.add(createProjectRegistrationInvitation(projectPath, i, registrationForm)));
		filterInvitations(allInv, enquiryForm, InvitationType.ENQUIRY).stream()
				.forEach(i -> ret.add(createProjectEnquiryInvitation(projectPath, i, enquiryForm)));

		return ret;
	}

	private List<InvitationWithCode> filterInvitations(List<InvitationWithCode> allInv, BaseForm form,
			InvitationType type)
	{
		if (form == null)
			return Collections.emptyList();

		return allInv.stream()
				.filter(f -> f.getInvitation().getType().equals(type)
						&& f.getInvitation().getFormId().equals(form.getName()))
				.collect(Collectors.toList());
	}

	private ProjectInvitation createProjectRegistrationInvitation(String projectPath, InvitationWithCode invitation,
			RegistrationForm form)
	{
		return new ProjectInvitation(projectPath, invitation, PublicRegistrationURLSupport
				.getPublicRegistrationLink(form, invitation.getRegistrationCode(), sharedEndpointMan));
	}

	private ProjectInvitation createProjectEnquiryInvitation(String projectPath, InvitationWithCode invitation,
			EnquiryForm form)
	{
		return new ProjectInvitation(projectPath, invitation, PublicRegistrationURLSupport
				.getPublicEnquiryLink(form, invitation.getRegistrationCode(), sharedEndpointMan));
	}

	private GroupDelegationConfiguration getDelegationConfiguration(String projectPath) throws EngineException
	{
		GroupContents contents = groupMan.getContents(projectPath, GroupContents.METADATA);
		return contents.getGroup().getDelegationConfiguration();
	}
	
	private String getProjectDisplayedName(String projectPath) throws EngineException
	{
		return groupMan.getContents(projectPath, GroupContents.METADATA).getGroup().getDisplayedName().getValue(msg);
	}

	@Override
	public void removeInvitation(String projectPath, String code) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath);
		assertIfIsProjectInvitation(projectPath, code);
		invitationMan.removeInvitation(code);
	}

	private String getRegistrationFormForProject(String projectPath) throws EngineException
	{
		GroupDelegationConfiguration config = getDelegationConfiguration(projectPath);
		if (config.registrationForm == null || config.registrationForm.isEmpty())
			throw new ProjectMisconfiguredException(projectPath);
		
		return config.registrationForm;
	}

	private String getEnquiryFormForProject(String projectPath) throws EngineException
	{
		GroupDelegationConfiguration config = getDelegationConfiguration(projectPath);
		if (config.signupEnquiryForm == null || config.signupEnquiryForm.isEmpty())
				throw new ProjectMisconfiguredException(projectPath);

		return config.signupEnquiryForm;
	}

	@Override
	public void sendInvitation(String projectPath, String code) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath);

		InvitationWithCode orgInvitationWithCode = assertIfIsProjectInvitation(projectPath, code);
		InvitationParam orgInvitation = orgInvitationWithCode.getInvitation();

		if (orgInvitation.isExpired())
		{
			Instant creationTime = orgInvitationWithCode.getCreationTime();
			Instant newExpiration = Instant.now();
			if (creationTime != null)
			{
				Duration between = Duration.between(creationTime, orgInvitation.getExpiration());
				newExpiration = newExpiration.plus(between);
			} else
			{
				newExpiration = newExpiration.plus(ProjectInvitation.DEFAULT_TTL_DAYS, ChronoUnit.DAYS);
			}

			InvitationParam newInvitation = null;

			if (orgInvitation.getType().equals(InvitationType.REGISTRATION))
			{
				RegistrationInvitationParam rnewInvitation = new RegistrationInvitationParam(
						orgInvitation.getFormId(), newExpiration,
						orgInvitation.getContactAddress());
				rnewInvitation.setExpectedIdentity(
						((RegistrationInvitationParam) orgInvitation).getExpectedIdentity());
				newInvitation = rnewInvitation;
			} else
			{
				EnquiryInvitationParam enewInvitation = new EnquiryInvitationParam(
						orgInvitation.getFormId(), newExpiration,
						orgInvitation.getContactAddress());
				enewInvitation.setEntity(((EnquiryInvitationParam) orgInvitation).getEntity());
				newInvitation = enewInvitation;
			}
			newInvitation.getGroupSelections().putAll(orgInvitation.getGroupSelections());
			newInvitation.getAllowedGroups().putAll(orgInvitation.getAllowedGroups());
			newInvitation.getAttributes().putAll(orgInvitation.getAttributes());
			newInvitation.getIdentities().putAll(orgInvitation.getIdentities());
			newInvitation.getMessageParams().putAll(orgInvitation.getMessageParams());

			String newCode = invitationMan.addInvitation(newInvitation);
			invitationMan.sendInvitation(newCode);
			invitationMan.removeInvitation(orgInvitationWithCode.getRegistrationCode());
		} else
		{
			invitationMan.sendInvitation(orgInvitationWithCode.getRegistrationCode());
		}
	}

	private InvitationWithCode assertIfIsProjectInvitation(String projectPath, String code) throws EngineException
	{
		GroupDelegationConfiguration config = getDelegationConfiguration(projectPath);

		Optional<InvitationWithCode> invO = invitationMan.getInvitations().stream()
				.filter(i -> i.getRegistrationCode().equals(code)).findFirst();
		if (!invO.isPresent())
			throw new IllegalInvitationException(code);

		InvitationWithCode orgInvitationWithCode = invO.get();
		InvitationParam invParam = orgInvitationWithCode.getInvitation();

		if (invParam == null || !(invParam.getFormId().equals(config.registrationForm)
				|| invParam.getFormId().equals(config.signupEnquiryForm)))
		{
			throw new NotProjectInvitation(projectPath, orgInvitationWithCode.getRegistrationCode());
		}

		return orgInvitationWithCode;
	}
}
