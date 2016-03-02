/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.translation.TranslationProfile;

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
	private DBIdentities dbIdentities;
	private RegistrationConfirmationSupport confirmationsSupport;
	private InternalFacilitiesManagement facilitiesManagement;
	private RegistrationRequestValidator registrationRequestValidator;
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;


	@Autowired
	public SharedRegistrationManagment(UnityMessageSource msg,
			NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, DBGroups dbGroups,
			EngineHelper engineHelper, RegistrationRequestDB requestDB,
			DBIdentities dbIdentities,
			RegistrationConfirmationSupport confirmationsSupport,
			InternalFacilitiesManagement facilitiesManagement,
			RegistrationRequestValidator registrationRequestValidator,
			RegistrationActionsRegistry registrationTranslationActionsRegistry)
	{
		super(msg, notificationProducer, attributesHelper, dbGroups, engineHelper);
		this.requestDB = requestDB;
		this.dbIdentities = dbIdentities;
		this.confirmationsSupport = confirmationsSupport;
		this.facilitiesManagement = facilitiesManagement;
		this.registrationRequestValidator = registrationRequestValidator;
		this.registrationTranslationActionsRegistry = registrationTranslationActionsRegistry;
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
			boolean rewriteConfirmationToken, SqlSession sql) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.accepted);

		RegistrationTranslationProfile translationProfile = getRegistrationProfileInstance(
				form.getTranslationProfile());
		TranslatedRegistrationRequest translatedRequest = translationProfile.translate(form, currentRequest);
		
		registrationRequestValidator.validateTranslatedRequest(form, currentRequest.getRequest(), 
				translatedRequest, sql);
		requestDB.update(currentRequest.getRequestId(), currentRequest, sql);

		List<Attribute<?>> rootAttributes = new ArrayList<>(translatedRequest.getAttributes().size());
		Map<String, List<Attribute<?>>> remainingAttributesByGroup = new HashMap<String, List<Attribute<?>>>();
		for (Attribute<?> a : translatedRequest.getAttributes())
			addAttributeToGroupsMap(a, rootAttributes, remainingAttributesByGroup);

		Collection<IdentityParam> identities = translatedRequest.getIdentities();
		Iterator<IdentityParam> identitiesIterator = identities.iterator();
		
		Identity initial = engineHelper.addEntity(identitiesIterator.next(),
				translatedRequest.getCredentialRequirement(),
				translatedRequest.getEntityState(), 
				false, rootAttributes, true, sql);

		while (identitiesIterator.hasNext())
		{
			IdentityParam idParam = identitiesIterator.next();
			dbIdentities.insertIdentity(idParam, initial.getEntityId(), false, sql);
		}

		applyRequestedGroups(initial.getEntityId(), remainingAttributesByGroup, 
				translatedRequest, sql);
		applyRequestedAttributeClasses(translatedRequest, initial.getEntityId(), sql);		
		applyRequestedCredentials(currentRequest, initial.getEntityId(), sql);
		
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		sendProcessingNotification(notificationsCfg.getAcceptedTemplate(), currentRequest,
				form.getName(), true, publicComment,
				internalComment, notificationsCfg, sql);
		if (rewriteConfirmationToken)
			confirmationsSupport.rewriteRequestToken(currentRequest, initial.getEntityId());

		return initial.getEntityId();
	}
	
	public void dropRequest(String id, SqlSession sql) throws EngineException
	{
		requestDB.remove(id, sql);
	}
	
	public void rejectRequest(RegistrationForm form, RegistrationRequestState currentRequest, 
			AdminComment publicComment, AdminComment internalComment, SqlSession sql) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.rejected);
		requestDB.update(currentRequest.getRequestId(), currentRequest, sql);
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		String receipentAddress = getRequesterAddress(currentRequest, notificationsCfg, sql);
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
	public Long autoProcess(RegistrationForm form, RegistrationRequestState requestFull, String logMessageTemplate,
			SqlSession sql)	throws EngineException
	{
		RegistrationTranslationProfile translationProfile = getRegistrationProfileInstance(
				form.getTranslationProfile());
		
		AutomaticRequestAction autoProcessAction = translationProfile.getAutoProcessAction(
				form, requestFull, RequestSubmitStatus.submitted);
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
			return acceptRequest(form, requestFull, null, systemComment, false, sql);
		case drop:
			dropRequest(requestFull.getRequestId(), sql);
			break;
		case reject:
			requestFull.getAdminComments().add(systemComment);
			rejectRequest(form, requestFull, null, systemComment, sql);
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
			RegistrationFormNotifications notificationsCfg, SqlSession sql)
			throws EngineException
	{
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg, sql);
		sendProcessingNotification(templateId, currentRequest, formId, sendToRequester, 
				publicComment, internalComment, notificationsCfg, requesterAddress);
	}

	private String getRequesterAddress(RegistrationRequestState currentRequest,
			RegistrationFormNotifications notificationsCfg, SqlSession sql)
			throws EngineException
	{
		NotificationFacility notificationFacility = facilitiesManagement.getNotificationFacilityForChannel(
				notificationsCfg.getChannel(), sql);
		return notificationFacility.getAddressForRegistrationRequest(currentRequest, sql);
	}

	
	private RegistrationTranslationProfile getRegistrationProfileInstance(TranslationProfile profile)
	{
		return new RegistrationTranslationProfile(profile.getName(), profile.getRules(), 
				registrationTranslationActionsRegistry);
	}
}
