/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.GroupDiffUtils;
import pl.edu.icm.unity.engine.api.registration.RequestSubmitStatus;
import pl.edu.icm.unity.engine.api.registration.RequestedGroupDiff;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
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
import pl.edu.icm.unity.engine.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

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
	
	private RegistrationRequestPreprocessor registrationRequestValidator;
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	private IdentityHelper identityHelper;
	private AttributeTypeHelper atHelper;
	private RegistrationConfirmationSupport confirmationsSupport;
	private AutomaticInvitationProcessingSupport autoInvitationProcessingSupport;
	private GroupDAO groupDB;

	@Autowired
	public SharedRegistrationManagment(UnityMessageSource msg,
			NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, GroupHelper groupHelper,
			EntityCredentialsHelper entityCredentialsHelper,
			RegistrationRequestDB requestDB,
			RegistrationConfirmationRewriteSupport confirmationsRewriteSupport,
			InternalFacilitiesManagement facilitiesManagement,
			RegistrationRequestPreprocessor registrationRequestValidator,
			RegistrationActionsRegistry registrationTranslationActionsRegistry,
			IdentityHelper identityHelper,
			AttributeTypeHelper atHelper,
			RegistrationConfirmationSupport confirmationsSupport,
			AutomaticInvitationProcessingSupport autoInvitationProcessingSupport,
			InvitationDB invitationDB,
			GroupDAO groupDB)
			
	{
		super(msg, notificationProducer, attributesHelper, groupHelper,
				entityCredentialsHelper, facilitiesManagement, invitationDB);
		this.requestDB = requestDB;
		this.confirmationsRewriteSupport = confirmationsRewriteSupport;
		this.registrationRequestValidator = registrationRequestValidator;
		this.registrationTranslationActionsRegistry = registrationTranslationActionsRegistry;
		this.identityHelper = identityHelper;
		this.atHelper = atHelper;
		this.confirmationsSupport = confirmationsSupport;
		this.autoInvitationProcessingSupport = autoInvitationProcessingSupport;
		this.groupDB = groupDB;
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
		
		autoInvitationProcessingSupport.autoProcessInvitationsAndCollectData(
				currentRequest, translatedRequest, groupParamByPath, requestedAttributes, form.getTranslationProfile().getName());
		
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
		
		RequestedGroupDiff diff = GroupDiffUtils.getAllRequestedGroupsDiff(groupDB.getAll(),
				Collections.emptyList(), currentRequest.getRequest().getGroupSelections(),
				form.getGroupParams());

		List<GroupParam> toAdd = groupParamByPath.values().stream()
				.filter(p -> diff.toAdd.contains(p.getGroup()))
				.collect(Collectors.toList());
		toAdd.addAll(groupParamByPath.values().stream().filter(g -> g.getTranslationProfile() != null).collect(Collectors.toList()));
		
		applyRequestedGroups(initial.getEntityId(), remainingAttributesByGroup, toAdd, null);
		applyRequestedAttributeClasses(translatedRequest.getAttributeClasses(), initial.getEntityId());		
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
			throw new RuntimeEngineException("Auto processing of registration form in result of async event failed", e);
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
}
