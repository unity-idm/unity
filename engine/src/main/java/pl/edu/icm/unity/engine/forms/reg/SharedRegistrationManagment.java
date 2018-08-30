/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.base.Functions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.RequestSubmitStatus;
import pl.edu.icm.unity.engine.api.translation.form.AutomaticInvitationProcessingParam;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.forms.BaseSharedRegistrationSupport;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport.Phase;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.engine.identity.IdentityHelper;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.engine.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

/**
 * Implementation of the internal registration management. This is used
 * internally and not exposed by the public interfaces.
 * 
 * @author P. Piernik
 */
@Component
public class SharedRegistrationManagment extends BaseSharedRegistrationSupport
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER,
			SharedRegistrationManagment.class);

	private RegistrationRequestDB requestDB;
	private RegistrationConfirmationRewriteSupport confirmationsRewriteSupport;
	
	private RegistrationRequestValidator registrationRequestValidator;
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	private IdentityHelper identityHelper;
	private AttributeTypeHelper atHelper;
	private RegistrationConfirmationSupport confirmationsSupport;
	private InvitationManagement invitationManagement;
	private RegistrationFormDB formsDB;

	@Autowired
	public SharedRegistrationManagment(UnityMessageSource msg,
			NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, GroupHelper groupHelper,
			EntityCredentialsHelper entityCredentialsHelper,
			RegistrationRequestDB requestDB,
			RegistrationConfirmationRewriteSupport confirmationsRewriteSupport,
			InternalFacilitiesManagement facilitiesManagement,
			RegistrationRequestValidator registrationRequestValidator,
			RegistrationActionsRegistry registrationTranslationActionsRegistry,
			IdentityHelper identityHelper,
			AttributeTypeHelper atHelper,
			RegistrationConfirmationSupport confirmationsSupport,
			InvitationManagement invitationManagement,
			RegistrationFormDB formsDB)
			
	{
		super(msg, notificationProducer, attributesHelper, groupHelper,
				entityCredentialsHelper, facilitiesManagement);
		this.requestDB = requestDB;
		this.confirmationsRewriteSupport = confirmationsRewriteSupport;
		this.registrationRequestValidator = registrationRequestValidator;
		this.registrationTranslationActionsRegistry = registrationTranslationActionsRegistry;
		this.identityHelper = identityHelper;
		this.atHelper = atHelper;
		this.confirmationsSupport = confirmationsSupport;
		this.invitationManagement = invitationManagement;
		this.formsDB = formsDB;
	}

	/**
	 * Accepts a registration request applying all its settings. The method operates on a result 
	 * of the form's translation profile, rather then on the original request. 
	 * @param form
	 * @param currentRequest
	 * @param publicComment
	 * @param internalComment
	 * @param rewriteConfirmationToken
	 * @param sql
	 * @return
	 * @throws EngineException
	 */
	public Long acceptRequest(RegistrationForm form, RegistrationRequestState currentRequest,
			AdminComment publicComment, AdminComment internalComment,
			boolean rewriteConfirmationToken) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.accepted);

		RegistrationTranslationProfile translationProfile = new RegistrationTranslationProfile(
				form.getTranslationProfile(), registrationTranslationActionsRegistry, atHelper, form);
		TranslatedRegistrationRequest translatedRequest = translationProfile.translate(currentRequest);
		
		registrationRequestValidator.validateTranslatedRequest(form, currentRequest.getRequest(), 
				translatedRequest);
		
		Map<String, GroupParam> groupParamByPath = translatedRequest.getGroups().stream()
				.collect(Collectors.toMap(GroupParam::getGroup, Functions.identity()));
		List<Attribute> requestedAttributes = Lists.newArrayList(translatedRequest.getAttributes());
		
		autoProcessInvitationsAndCollectData(currentRequest, translatedRequest, groupParamByPath, requestedAttributes);
		
		List<Attribute> rootAttributes = new ArrayList<>(translatedRequest.getAttributes().size());
		Map<String, List<Attribute>> remainingAttributesByGroup = new HashMap<>();
		for (Attribute a : requestedAttributes)
			addAttributeToGroupsMap(a, rootAttributes, remainingAttributesByGroup);

		Collection<IdentityParam> identities = translatedRequest.getIdentities();
		Iterator<IdentityParam> identitiesIterator = identities.iterator();
		
		Identity initial = identityHelper.addEntity(identitiesIterator.next(),
				translatedRequest.getCredentialRequirement(),
				translatedRequest.getEntityState(), 
				false, rootAttributes, true);

		currentRequest.setCreatedEntityId(initial.getEntityId());
		requestDB.update(currentRequest);
		
		while (identitiesIterator.hasNext())
		{
			IdentityParam idParam = identitiesIterator.next();
			identityHelper.insertIdentity(idParam, initial.getEntityId(), false);
		}

		applyRequestedGroups(initial.getEntityId(), remainingAttributesByGroup, groupParamByPath.values());
		applyRequestedAttributeClasses(translatedRequest, initial.getEntityId());		
		applyRequestedCredentials(currentRequest, initial.getEntityId());
		
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		sendProcessingNotification(notificationsCfg.getAcceptedTemplate(), currentRequest,
				form.getName(), true, publicComment,
				internalComment, notificationsCfg);
		confirmationsSupport.sendAttributeConfirmationRequest(currentRequest, initial.getEntityId(), form,
				Phase.ON_ACCEPT);
		confirmationsSupport.sendIdentityConfirmationRequest(currentRequest, initial.getEntityId(), form,
				Phase.ON_ACCEPT);
		if (rewriteConfirmationToken)
			confirmationsRewriteSupport.rewriteRequestToken(currentRequest, initial.getEntityId());
		
		return initial.getEntityId();
	}

	private void autoProcessInvitationsAndCollectData(RegistrationRequestState currentRequest,
			TranslatedRegistrationRequest translatedRequest, Map<String, GroupParam> groupParamByPath,
			List<Attribute> requestedAttributes) throws EngineException
	{
		AutomaticInvitationProcessingParam invitationProcessing = translatedRequest.getInvitationProcessing();
		if (invitationProcessing == null)
			return;
		
		CollectedFromInvitationsContainer collectedFromInvitations = collectAttributesAndGroupsFromInvitations(
				currentRequest, translatedRequest);
		
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
			TranslatedRegistrationRequest translatedRequest) throws EngineException
	{
		NotificationFacility facility = facilitiesManagement.getNotificationFacilityForChannel(
				UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL);
		String contactAddress = facility.getAddressForUserRequest(currentRequest);
		if (contactAddress == null)
			return null;
		
		AutomaticInvitationProcessingParam invitationProcessing = translatedRequest.getInvitationProcessing();
		List<InvitationWithCode> invitationsToProcess = invitationManagement.getInvitations().stream()
			.filter(byGivenFormOrAllIfEmpty(invitationProcessing.getFormName()))
			.filter(invitation -> contactAddress.equals(invitation.getContactAddress()))
			.collect(Collectors.toList());
		Map<String, RegistrationForm> registrationFormById = Maps.newHashMap();
		
		CollectedFromInvitationsContainer collected = new CollectedFromInvitationsContainer();
		for (InvitationWithCode invitation : invitationsToProcess)
		{
			RegistrationForm invitationRegistrationForm = registrationFormById.get(invitation.getFormId());
			if (invitationRegistrationForm == null)
			{
				invitationRegistrationForm = formsDB.get(invitation.getFormId());
				registrationFormById.put(invitation.getFormId(), invitationRegistrationForm);
			}
			List<Attribute> prefilledAttrs = RegistrationUtil.getPrefilledAndHiddenAttributes(invitation);
			collected.attributes.addAll(prefilledAttrs);
			List<GroupParam> prefilledGroups = RegistrationUtil.getPrefilledAndHiddenGroups(invitation, invitationRegistrationForm);
			collected.groups.addAll(prefilledGroups);
			collected.registrationCodes.add(invitation.getRegistrationCode());
		}
		return collected;
	}
	
	static class CollectedFromInvitationsContainer
	{
		Set<Attribute> attributes = Sets.newHashSet();
		Set<GroupParam> groups = Sets.newHashSet();
		Set<String> registrationCodes = Sets.newHashSet();
	}

	public void dropRequest(String id) throws EngineException
	{
		requestDB.delete(id);
	}
	
	public void rejectRequest(RegistrationForm form, RegistrationRequestState currentRequest, 
			AdminComment publicComment, AdminComment internalComment) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.rejected);
		requestDB.update(currentRequest);
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		String templateId = notificationsCfg.getRejectedTemplate();
		String receipentAddress = getRequesterAddress(currentRequest, templateId);
		sendProcessingNotification(templateId, currentRequest, form.getName(), true,
				publicComment, internalComment, notificationsCfg, receipentAddress);
	}
	
	/**
	 * Basing on the profile's decision automatically process the request if needed.
	 * @return entity id if automatic request acceptance was performed.
	 * @throws EngineException 
	 */
	public Long autoProcess(RegistrationForm form, RegistrationRequestState requestFull, 
			String logMessageTemplate)	throws EngineException
	{
		RegistrationTranslationProfile translationProfile = new RegistrationTranslationProfile(
				form.getTranslationProfile(), registrationTranslationActionsRegistry, atHelper, form);
		
		AutomaticRequestAction autoProcessAction = translationProfile.getAutoProcessAction(
				requestFull, RequestSubmitStatus.submitted);
		if (autoProcessAction == AutomaticRequestAction.none)
			return null;

		AdminComment systemComment = new AdminComment(
				SharedRegistrationManagment.AUTO_PROCESS_COMMENT, 0, false);

		String formattedMsg = MessageFormat.format(logMessageTemplate, autoProcessAction);
		LOG.info(formattedMsg);
		
		switch (autoProcessAction)
		{
		case accept:
			requestFull.getAdminComments().add(systemComment);
			return acceptRequest(form, requestFull, null, systemComment, false);
		case drop:
			dropRequest(requestFull.getRequestId());
			break;
		case reject:
			requestFull.getAdminComments().add(systemComment);
			rejectRequest(form, requestFull, null, systemComment);
			break;
		default:
		}
		return null;
	}

	@EventListener
	public void onAutoProcessEvent(RegistrationRequestAutoProcessEvent event)
	{
		try
		{
			autoProcess(event.form, event.requestFull, event.logMessageTemplate);
		} catch (EngineException e)
		{
			LOG.error("Auto processing of registration form in result of async event failed", e);
		}
	}
	
	/**
	 * Creates and sends notifications to the requester and admins in effect
	 * of request processing.
	 * 
	 * @param sendToRequester
	 *                if true then the notification is sent to requester if
	 *                only we have its address. If false, then notification
	 *                is sent to requester only if we have its address and
	 *                if a public comment was given.
	 * @throws EngineException
	 */
	public void sendProcessingNotification(String templateId,
			RegistrationRequestState currentRequest, String formId,
			boolean sendToRequester, AdminComment publicComment,
			AdminComment internalComment,
			RegistrationFormNotifications notificationsCfg)
			throws EngineException
	{
		if (templateId == null || templateId.isEmpty())
				return;
		String requesterAddress = getRequesterAddress(currentRequest, templateId);
		sendProcessingNotification(templateId, currentRequest, formId, sendToRequester, 
				publicComment, internalComment, notificationsCfg, requesterAddress);
	}

	private String getRequesterAddress(RegistrationRequestState currentRequest,
			String templateId) throws EngineException
	{
		if (templateId == null || templateId.isEmpty())
			return null;
		
		NotificationFacility notificationFacility = facilitiesManagement
				.getNotificationFacilityForMessageTemplate(templateId);
		if (notificationFacility == null)
			return null;
		return notificationFacility.getAddressForUserRequest(currentRequest);
	}
	
	private Predicate<? super InvitationWithCode> byGivenFormOrAllIfEmpty(String formName)
	{
		return invitation ->
		{
			if (Strings.isNullOrEmpty(formName))
				return true;
			return formName.equals(invitation.getFormId());
		};
	}
}
