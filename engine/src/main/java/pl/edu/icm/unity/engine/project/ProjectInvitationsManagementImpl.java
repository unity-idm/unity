/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.invitation.ComboInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.api.identity.UnknownEmailException;
import pl.edu.icm.unity.engine.api.project.ProjectAddInvitationResult;
import pl.edu.icm.unity.engine.api.project.ProjectInvitation;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;


@Component
@Primary
public class ProjectInvitationsManagementImpl implements ProjectInvitationsManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FORMS,
			ProjectInvitationsManagementImpl.class);
	
	public static final String INVITATION_PROJECT_NAME_PARAM = "upmanProject";
	
	private final ProjectAuthorizationManager authz;
	private final InvitationManagement invitationMan;
	private final GroupsManagement groupMan;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final RegistrationsManagement registrationMan;
	private final EnquiryManagement enquiryMan;
	private final EntityManagement entityMan;
	private final MessageSource msg;
	
	public ProjectInvitationsManagementImpl(@Qualifier("insecure") InvitationManagement invitationMan,
			@Qualifier("insecure") GroupsManagement groupMan,
			@Qualifier("insecure") RegistrationsManagement registrationMan,
			@Qualifier("insecure") EnquiryManagement enquiryMan,
			@Qualifier("insecure") EntityManagement entityMan,
			PublicRegistrationURLSupport publicRegistrationURLSupport, 
			ProjectAuthorizationManager authz,
			MessageSource msg)
	{
		this.invitationMan = invitationMan;
		this.groupMan = groupMan;
		this.entityMan = entityMan;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
		this.registrationMan = registrationMan;
		this.enquiryMan = enquiryMan;
		this.authz = authz;
		this.msg = msg;
	}

	@Override
	public ProjectAddInvitationResult addInvitations(Set<ProjectInvitationParam> invitationParams) throws EngineException
	{
		for (String project: invitationParams.stream().map(p -> p.project).collect(Collectors.toSet()))
		{
			authz.assertManagerAuthorization(project);
		}
		
		Set<EntityWithContactInfo> entities = null;
		try
		{
			entities = entityMan.getAllEntitiesWithContactEmails(invitationParams.stream()
					.map(p -> p.contactAddress)
					.collect(Collectors.toSet()));
		} catch (UnknownEmailException e)
		{
			// ok
		}
		
		
		Set<ProjectInvitationParam> toSkip = new HashSet<>();
		
		if (entities != null && !entities.isEmpty())
		{
			for (ProjectInvitationParam invitation : invitationParams)
			{
				if (checkIsAlreadyMember(entities.stream()
						.filter(e -> e.contactEmail.equals(invitation.contactAddress))
						.collect(Collectors.toSet()), invitation.project))
				{
					toSkip.add(invitation);
				}
			}
		}
		for (ProjectInvitationParam param : invitationParams.stream().filter(i -> !toSkip.contains(i)).collect(Collectors.toSet()))
		{
			String code = invitationMan.addInvitation(createComboInvitation(param));
			invitationMan.sendInvitation(code);
		}
	
		return ProjectAddInvitationResult.builder()
				.withProjectAlreadyMemberEmails(toSkip.stream()
						.map(a -> a.contactAddress)
						.collect(Collectors.toSet()))
				.build();
		
	}

	private boolean checkIsAlreadyMember(Set<EntityWithContactInfo> collect, String project)
	{
		if (collect.stream().filter(e -> e.groups.contains(project)).findAny().isPresent())
		{
			return true;
		}
		return false;
	}

	private ComboInvitationParam createComboInvitation(ProjectInvitationParam param) throws EngineException
	{
		ComboInvitationParam invitationParam = new ComboInvitationParam(getRegistrationFormForProject(param.project),
				getEnquiryFormForProject(param.project), param.expiration, param.contactAddress);
		
		fillGroups(invitationParam.getEnquiryFormPrefill(), param);
		fillGroups(invitationParam.getRegistrationFormPrefill(), param);
		fillProjectDisplayedNameParam(invitationParam.getRegistrationFormPrefill().getMessageParams(), param);
		fillProjectDisplayedNameParam(invitationParam.getEnquiryFormPrefill().getMessageParams(), param);
		
		IdentityParam emailId = new IdentityParam(EmailIdentity.ID, param.contactAddress);
		emailId.setConfirmationInfo(new ConfirmationInfo(true));
		invitationParam.getRegistrationFormPrefill().getIdentities().put(0, new PrefilledEntry<>(emailId, PrefilledEntryMode.HIDDEN));
		
		return invitationParam;	
	}
	
	private void fillProjectDisplayedNameParam(Map<String, String> msgParamsToSet, ProjectInvitationParam param) throws EngineException
	{
		msgParamsToSet.put(
				MessageTemplateDefinition.CUSTOM_VAR_PREFIX + INVITATION_PROJECT_NAME_PARAM,
				getProjectDisplayedName(param.project));
	}
	
	private void fillGroups(FormPrefill toSet, ProjectInvitationParam param)
	{
		if (param.groups == null || param.groups.isEmpty())
		{
			toSet.getGroupSelections().put(0, new PrefilledEntry<>(new GroupSelection(Collections.emptyList()),
					PrefilledEntryMode.READ_ONLY));
			return;
		}

		if (param.allowModifyGroups)
		{
			toSet.getAllowedGroups().put(0, new GroupSelection(param.groups));
		} else
		{
			toSet.getGroupSelections().put(0, new PrefilledEntry<>(new GroupSelection(param.groups),
					PrefilledEntryMode.READ_ONLY));
		}
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
		
		
		for (InvitationWithCode invitation : filterInvitations(allInv, Arrays.asList(registrationForm), InvitationType.REGISTRATION))
		{
			ret.add(createProjectRegistrationInvitation(projectPath, invitation, registrationForm));
		}

		for (InvitationWithCode invitation : filterInvitations(allInv, Arrays.asList(enquiryForm), InvitationType.ENQUIRY))
		{
			ret.add(createProjectEnquiryInvitation(projectPath, invitation, enquiryForm));
		}
		
		for (InvitationWithCode invitation : filterInvitations(allInv, Arrays.asList(registrationForm, enquiryForm), InvitationType.COMBO))
		{
			ret.add(createProjectComboInvitation(projectPath, invitation, registrationForm));
		}
		

		return ret;
	}

	private List<InvitationWithCode> filterInvitations(List<InvitationWithCode> allInv, List<BaseForm> forms, InvitationType type)
	{
		if (forms.isEmpty())
			return Collections.emptyList();

		return allInv.stream()
				.filter(f -> {
					try
					{
						return f.getInvitation().getType().equals(type)
								&& formsMatch(f.getInvitation(), forms);
					} catch (IllegalFormTypeException e)
					{
						log.error("Invalid form type", e);
						return false;
					}
				})
				.collect(Collectors.toList());
	}
	
	private boolean formsMatch(InvitationParam invitation, List<BaseForm> forms) throws IllegalFormTypeException
	{
		boolean match = true;
		for (BaseForm form : forms)
		{
			match = match && invitation.matchesForm(form);
		}
		return match;
		
	}
	private ProjectInvitation createProjectRegistrationInvitation(String projectPath, InvitationWithCode invitation,
			RegistrationForm form) throws EngineException
	{
		return new ProjectInvitation(projectPath, form, invitation, publicRegistrationURLSupport
				.getPublicRegistrationLink(form.getName(), invitation.getRegistrationCode()));
	}

	private ProjectInvitation createProjectEnquiryInvitation(String projectPath, InvitationWithCode invitation,
			EnquiryForm form) throws EngineException
	{
		return new ProjectInvitation(projectPath, form, invitation, publicRegistrationURLSupport
				.getPublicEnquiryLink(form.getName(), invitation.getRegistrationCode()));
	}
	
	private ProjectInvitation createProjectComboInvitation(String projectPath, InvitationWithCode invitation,
			RegistrationForm form) throws EngineException
	{
		return new ProjectInvitation(projectPath, form, invitation, publicRegistrationURLSupport
				.getPublicRegistrationLink(form.getName(), invitation.getRegistrationCode()));
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
				newInvitation = copyRegistrationInvitation(newExpiration, orgInvitation);
			} else if (orgInvitation.getType().equals(InvitationType.ENQUIRY))
			{
				newInvitation = copyEnquiryInvitation(newExpiration, orgInvitation);
			} else
			{
				newInvitation = copyComboInvitation(newExpiration, orgInvitation);
			}

			String newCode = invitationMan.addInvitation(newInvitation);
			invitationMan.sendInvitation(newCode);
			invitationMan.removeInvitation(orgInvitationWithCode.getRegistrationCode());
		} else
		{
			invitationMan.sendInvitation(orgInvitationWithCode.getRegistrationCode());
		}
	}
	
	private InvitationParam copyRegistrationInvitation(Instant newExpiration, InvitationParam orgInvitation)
	{
		RegistrationInvitationParam orgRegistrationInvitationParam = (RegistrationInvitationParam) orgInvitation;
		return  orgRegistrationInvitationParam.cloningBuilder().withExpiration(newExpiration).build();
	}
	
	private InvitationParam copyEnquiryInvitation(Instant newExpiration, InvitationParam orgInvitation)
	{
		EnquiryInvitationParam orgEnquiryInvitationParam = (EnquiryInvitationParam) orgInvitation;
		return orgEnquiryInvitationParam.cloningBuilder().withExpiration(newExpiration).build();
	}
	
	private InvitationParam copyComboInvitation(Instant newExpiration, InvitationParam orgInvitation)
	{
		ComboInvitationParam orgComboInvitationParam = (ComboInvitationParam) orgInvitation;
		return 	orgComboInvitationParam.cloningBuilder().withExpiration(newExpiration).build();
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

		
		if (invParam == null)
		{
			throw new NotProjectInvitation(projectPath, orgInvitationWithCode.getRegistrationCode());
		}
		boolean matchReg = invParam.matchesForm(registrationMan.getForm(config.registrationForm));
		boolean matchEnq = invParam.matchesForm(enquiryMan.getEnquiry(config.signupEnquiryForm));
		if ((invParam.getType().equals(InvitationType.COMBO) && !(matchReg && matchEnq)) || !(matchReg || matchEnq))
		{
			throw new NotProjectInvitation(projectPath, orgInvitationWithCode.getRegistrationCode());
		}

		return orgInvitationWithCode;
	}
}
