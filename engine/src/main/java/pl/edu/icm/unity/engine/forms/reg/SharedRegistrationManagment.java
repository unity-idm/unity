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

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.RequestSubmitStatus;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.forms.BaseSharedRegistrationSupport;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.engine.identity.IdentityHelper;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.engine.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.exceptions.EngineException;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER,
			SharedRegistrationManagment.class);

	private RegistrationRequestDB requestDB;
	private RegistrationConfirmationRewriteSupport confirmationsSupport;
	private InternalFacilitiesManagement facilitiesManagement;
	private RegistrationRequestValidator registrationRequestValidator;
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	private IdentityHelper identityHelper;
	private AttributeTypeHelper atHelper;

	@Autowired
	public SharedRegistrationManagment(UnityMessageSource msg,
			NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, GroupHelper groupHelper,
			EntityCredentialsHelper entityCredentialsHelper,
			RegistrationRequestDB requestDB,
			RegistrationConfirmationRewriteSupport confirmationsSupport,
			InternalFacilitiesManagement facilitiesManagement,
			RegistrationRequestValidator registrationRequestValidator,
			RegistrationActionsRegistry registrationTranslationActionsRegistry,
			IdentityHelper identityHelper,
			AttributeTypeHelper atHelper)
	{
		super(msg, notificationProducer, attributesHelper, groupHelper,
				entityCredentialsHelper);
		this.requestDB = requestDB;
		this.confirmationsSupport = confirmationsSupport;
		this.facilitiesManagement = facilitiesManagement;
		this.registrationRequestValidator = registrationRequestValidator;
		this.registrationTranslationActionsRegistry = registrationTranslationActionsRegistry;
		this.identityHelper = identityHelper;
		this.atHelper = atHelper;
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

		List<Attribute> rootAttributes = new ArrayList<>(translatedRequest.getAttributes().size());
		Map<String, List<Attribute>> remainingAttributesByGroup = new HashMap<>();
		for (Attribute a : translatedRequest.getAttributes())
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

		applyRequestedGroups(initial.getEntityId(), remainingAttributesByGroup, 
				translatedRequest);
		applyRequestedAttributeClasses(translatedRequest, initial.getEntityId());		
		applyRequestedCredentials(currentRequest, initial.getEntityId());
		
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		sendProcessingNotification(notificationsCfg.getAcceptedTemplate(), currentRequest,
				form.getName(), true, publicComment,
				internalComment, notificationsCfg);
		if (rewriteConfirmationToken)
			confirmationsSupport.rewriteRequestToken(currentRequest, initial.getEntityId());

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
		String receipentAddress = getRequesterAddress(currentRequest, notificationsCfg);
		sendProcessingNotification(notificationsCfg.getRejectedTemplate(), 
				currentRequest, form.getName(),
				true, publicComment, 
				internalComment, notificationsCfg, receipentAddress);
	}
	
	/**
	 * Basing on the profile's decision automatically process the request if needed.
	 * @return entity id if automatic request acceptance was performed.
	 * @throws EngineException 
	 */
	public Long autoProcess(RegistrationForm form, RegistrationRequestState requestFull, String logMessageTemplate)	
			throws EngineException
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
		log.info(formattedMsg);
		
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
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg);
		sendProcessingNotification(templateId, currentRequest, formId, sendToRequester, 
				publicComment, internalComment, notificationsCfg, requesterAddress);
	}

	private String getRequesterAddress(RegistrationRequestState currentRequest,
			RegistrationFormNotifications notificationsCfg)
			throws EngineException
	{
		if (notificationsCfg.getChannel() == null)
			return null;
		NotificationFacility notificationFacility = facilitiesManagement.getNotificationFacilityForChannel(
				notificationsCfg.getChannel());
		return notificationFacility.getAddressForUserRequest(currentRequest);
	}
}
