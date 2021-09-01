/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import static java.util.stream.Collectors.toCollection;
import static pl.edu.icm.unity.engine.forms.reg.RegistrationUtil.getPrefilledAndHiddenAttributes;
import static pl.edu.icm.unity.engine.forms.reg.RegistrationUtil.getPrefilledAndHiddenGroups;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.translation.form.AutomaticInvitationProcessingParam;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessInvitationsActionFactory.AutoProcessInvitationsAction;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

/**
 * Helper component that helps with processing a registration request from
 * {@link AutoProcessInvitationsAction} point of view.
 * 
 * If an translation action applies to registration request, it collects
 * attributes and groups from invitations that are active for processed entity,
 * and deletes the invitations.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
class AutomaticInvitationProcessingSupport
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_FORMS, AutomaticInvitationProcessingSupport.class);
	
	private InvitationManagement invitationManagement;
	private RegistrationFormDB formsDB;
	private InternalFacilitiesManagement facilitiesManagement;
	
	
	@Autowired
	public AutomaticInvitationProcessingSupport(@Qualifier("insecure") InvitationManagement invitationManagement, 
			RegistrationFormDB formsDB,
			InternalFacilitiesManagement facilitiesManagement)
	{
		this.invitationManagement = invitationManagement;
		this.formsDB = formsDB;
		this.facilitiesManagement = facilitiesManagement;
	}

	void autoProcessInvitationsAndCollectData(RegistrationRequestState currentRequest,
			TranslatedRegistrationRequest translatedRequest, Map<String, GroupParam> groupParamByPath,
			List<Attribute> requestedAttributes, String profileName) throws EngineException
	{
		List<AutomaticInvitationProcessingParam> invitationProcessing = translatedRequest.getInvitationProcessingParams();
		if (CollectionUtils.isEmpty(invitationProcessing))
			return;
		
		CollectedFromInvitationsContainer collectedFromInvitations = collectAttributesAndGroupsFromInvitations(
				currentRequest, translatedRequest, profileName);
		
		Set<String> groupsAdded = Sets.newHashSet();
		for (GroupParam group : collectedFromInvitations.groups)
		{
			if (!groupParamByPath.containsKey(group.getGroup()))
			{
				groupParamByPath.put(group.getGroup(), group);
				groupsAdded.add(group.getGroup());
			}
		}
		requestedAttributes.addAll(collectedFromInvitations.attributes);
		
		for (String code : collectedFromInvitations.registrationCodes)
			invitationManagement.removeInvitation(code);
		
		String adminMsg = String.format("%s: %s", SharedRegistrationManagment.AUTO_PROCESS_INVITATIONS_COMMENT,
				collectedFromInvitations.registrationCodes.stream().collect(Collectors.joining(",")));
		AdminComment systemComment = new AdminComment(adminMsg, 0, false);
		currentRequest.getAdminComments().add(systemComment);
		
		logSummary(adminMsg, groupsAdded, collectedFromInvitations);
	}

	private void logSummary(String adminMsg, Set<String> groupsAdded, CollectedFromInvitationsContainer collectedFromInvitations)
	{
		StringBuilder summaryOfProcessing = new StringBuilder("Summary: ");
		summaryOfProcessing.append("added groups: ");
		if (groupsAdded.isEmpty())
			summaryOfProcessing.append(" -- none --.");
		else
			summaryOfProcessing.append(groupsAdded.stream().collect(Collectors.joining(",")));
		summaryOfProcessing.append("; added attributes: ");
		if (collectedFromInvitations.attributes.isEmpty())
			summaryOfProcessing.append(" -- none --.");
		else
			summaryOfProcessing.append(collectedFromInvitations.attributes.stream()
					.map(Attribute::toString).collect(Collectors.joining(",")));
		LOG.info("{}. {}", adminMsg, summaryOfProcessing.toString());
	}

	private CollectedFromInvitationsContainer collectAttributesAndGroupsFromInvitations(RegistrationRequestState currentRequest, 
			TranslatedRegistrationRequest translatedRequest, String profileName) throws EngineException
	{
		NotificationFacility facility = facilitiesManagement.getNotificationFacilityForChannel(
				UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL);
		String contactAddress = facility.getAddressForUserRequest(currentRequest);
		if (contactAddress == null)
			return null;
		
		Set<String> formsToProcess = translatedRequest.getInvitationProcessingParams().stream()
				.map(AutomaticInvitationProcessingParam::getFormName)
				.collect(toCollection(HashSet::new));
		Map<String, RegistrationForm> allFormsAsMap = formsDB.getAllAsMap();
		
		List<InvitationWithCode> invitationsToProcess = invitationManagement.getInvitations().stream()
			.filter(byGivenFormOrAllIfEmpty(formsToProcess, allFormsAsMap))
			.filter(invitation -> contactAddress.equals(invitation.getInvitation().getContactAddress()))
			.collect(Collectors.toList());
		Map<String, RegistrationForm> registrationFormById = Maps.newHashMap();
		
		
		CollectedFromInvitationsContainer collected = new CollectedFromInvitationsContainer();
		for (InvitationWithCode invitationWithCode : invitationsToProcess)
		{
			InvitationParam invitation = invitationWithCode.getInvitation();
			FormPrefill formInfo = invitation.getPrefillForAutoProcessing();
			
			RegistrationForm invitationRegistrationForm = registrationFormById.get(formInfo.getFormId());
			if (invitationRegistrationForm == null)
			{
				invitationRegistrationForm = formsDB.get(formInfo.getFormId());
				registrationFormById.put(formInfo.getFormId(), invitationRegistrationForm);
			}
			
			List<Attribute> prefilledAttrs = getPrefilledAndHiddenAttributes(formInfo, invitationRegistrationForm);
			collected.attributes.addAll(prefilledAttrs);
			List<GroupParam> prefilledGroups = getPrefilledAndHiddenGroups(formInfo, invitationRegistrationForm, profileName);
			collected.groups.addAll(prefilledGroups);
			collected.registrationCodes.add(invitationWithCode.getRegistrationCode());
		}
		return collected;
	}
	
	static class CollectedFromInvitationsContainer
	{
		Set<Attribute> attributes = Sets.newHashSet();
		Set<GroupParam> groups = Sets.newHashSet();
		Set<String> registrationCodes = Sets.newHashSet();
	}
	
	
	private Predicate<? super InvitationWithCode> byGivenFormOrAllIfEmpty(Set<String> formsToProcess, Map<String, RegistrationForm> allForms)
	{
		return invitation ->
		{
			if (formsToProcess.contains(null) || formsToProcess.contains(""))
				return true;
			
			for (String form : formsToProcess)
			{
				try
				{
					if  (invitation.getInvitation().matchesForm(allForms.get(form)))
						return true;
				} catch (IllegalFormTypeException e)
				{
					LOG.error("Invalid form type", e);
				}
			}
			
			return false;
		};
	}
}
