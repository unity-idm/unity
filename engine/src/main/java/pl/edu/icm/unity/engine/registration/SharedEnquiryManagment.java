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
import pl.edu.icm.unity.db.generic.reg.EnquiryResponseDB;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.EnquiryTranslationProfile;
import pl.edu.icm.unity.server.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Implementation of the shared code of enquires management. This class is used by the main manager 
 * implementing the public API and other facilities which trigger enquires processing.
 * 
 * @author P. Piernik
 */
@Component
public class SharedEnquiryManagment extends BaseSharedRegistrationSupport
{
	private static final Logger log = Log.getLogger(Log.U_SERVER,
			SharedEnquiryManagment.class);

	private EnquiryResponseDB enquiryResponseDB;
	private DBIdentities dbIdentities;
	private RegistrationConfirmationRewriteSupport confirmationsSupport;
	private InternalFacilitiesManagement facilitiesManagement;
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	private EnquiryResponseValidator responseValidator;
	
	@Autowired
	public SharedEnquiryManagment(UnityMessageSource msg,
			NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, DBGroups dbGroups,
			EngineHelper engineHelper, EnquiryResponseDB enquiryResponseDB,
			DBIdentities dbIdentities,
			RegistrationConfirmationRewriteSupport confirmationsSupport,
			InternalFacilitiesManagement facilitiesManagement,
			RegistrationActionsRegistry registrationTranslationActionsRegistry,
			EnquiryResponseValidator responseValidator)
	{
		super(msg, notificationProducer, attributesHelper, dbGroups, engineHelper);
		this.enquiryResponseDB = enquiryResponseDB;
		this.dbIdentities = dbIdentities;
		this.confirmationsSupport = confirmationsSupport;
		this.facilitiesManagement = facilitiesManagement;
		this.registrationTranslationActionsRegistry = registrationTranslationActionsRegistry;
		this.responseValidator = responseValidator;
	}

	/**
	 * Accepts a enquiry response applying all enquiry form rules. The method operates on a result 
	 * of the form's translation profile, rather then on the original request. 
	 * @param form
	 * @param currentRequest
	 * @param publicComment
	 * @param internalComment
	 * @param rewriteConfirmationToken
	 * @param sql
	 * @throws EngineException
	 */
	public void acceptEnquiryResponse(EnquiryForm form, EnquiryResponseState currentRequest,
			AdminComment publicComment, AdminComment internalComment,
			boolean rewriteConfirmationToken, SqlSession sql) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.accepted);

		EnquiryTranslationProfile translationProfile = getEnquiryProfileInstance(
				form.getTranslationProfile());
		TranslatedRegistrationRequest translatedRequest = translationProfile.translate(form, currentRequest);
		
		responseValidator.validateTranslatedRequest(form, currentRequest.getRequest(), 
				translatedRequest, sql);
		enquiryResponseDB.update(currentRequest.getRequestId(), currentRequest, sql);

		List<Attribute<?>> rootAttributes = new ArrayList<>(translatedRequest.getAttributes().size());
		Map<String, List<Attribute<?>>> remainingAttributesByGroup = new HashMap<String, List<Attribute<?>>>();
		for (Attribute<?> a : translatedRequest.getAttributes())
			addAttributeToGroupsMap(a, rootAttributes, remainingAttributesByGroup);

		long entityId = currentRequest.getEntityId();
		Collection<IdentityParam> identities = translatedRequest.getIdentities();
		Iterator<IdentityParam> identitiesIterator = identities.iterator();
		while (identitiesIterator.hasNext())
		{
			IdentityParam idParam = identitiesIterator.next();
			dbIdentities.insertIdentity(idParam, entityId, false, sql);
		}

		attributesHelper.addAttributesList(rootAttributes, entityId, true, sql);
		
		applyRequestedGroups(entityId, remainingAttributesByGroup, 
				translatedRequest, sql);

		applyRequestedAttributeClasses(translatedRequest, entityId, sql);
		
		applyRequestedCredentials(currentRequest, entityId, sql);
		
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg, sql);
		sendProcessingNotification(notificationsCfg.getAcceptedTemplate(), currentRequest,
				form.getName(), true, publicComment,
				internalComment, notificationsCfg, requesterAddress);
		if (rewriteConfirmationToken)
			confirmationsSupport.rewriteRequestToken(currentRequest, entityId);
	}
	
	public void dropEnquiryResponse(String id, SqlSession sql) throws EngineException
	{
		enquiryResponseDB.remove(id, sql);
	}
	
	public void rejectEnquiryResponse(EnquiryForm form, EnquiryResponseState currentRequest, 
			AdminComment publicComment, AdminComment internalComment, SqlSession sql) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.rejected);
		enquiryResponseDB.update(currentRequest.getRequestId(), currentRequest, sql);
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg, sql);
		sendProcessingNotification(notificationsCfg.getRejectedTemplate(), 
				currentRequest, form.getName(),
				true, publicComment, 
				internalComment, notificationsCfg, requesterAddress);
	}
	
	public void sendProcessingNotification(EnquiryForm form, String templateId,
			EnquiryResponseState currentRequest, String formId,
			AdminComment publicComment, AdminComment internalComment, SqlSession sql)
			throws EngineException
	{
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg, sql);
		sendProcessingNotification(templateId, currentRequest, formId, false, 
				publicComment, internalComment, notificationsCfg, requesterAddress);
	}
	
	/**
	 * Basing on the profile's decision automatically process the enquiry response if needed.
	 * @throws EngineException 
	 * @return true only if request was accepted
	 */
	public boolean autoProcessEnquiry(EnquiryForm form, EnquiryResponseState fullResponse, String logMessageTemplate,
			SqlSession sql)	throws EngineException
	{
		EnquiryTranslationProfile translationProfile = getEnquiryProfileInstance(
				form.getTranslationProfile());
		
		AutomaticRequestAction autoProcessAction = translationProfile.getAutoProcessAction(
				form, fullResponse, RequestSubmitStatus.submitted);
		if (autoProcessAction == AutomaticRequestAction.none)
			return false;
		
		AdminComment systemComment = new AdminComment(
				SharedEnquiryManagment.AUTO_PROCESS_COMMENT, 0, false);

		String formattedMsg = MessageFormat.format(logMessageTemplate, autoProcessAction);
		log.info(formattedMsg);
		
		switch (autoProcessAction)
		{
		case accept:
			acceptEnquiryResponse(form, fullResponse, null, systemComment, false, sql);
			return true;
		case drop:
			dropEnquiryResponse(fullResponse.getRequestId(), sql);
			break;
		case reject:
			fullResponse.getAdminComments().add(systemComment);
			rejectEnquiryResponse(form, fullResponse, null, systemComment, sql);
			break;
		default:
		}
		return false;
	}
	
	private String getRequesterAddress(EnquiryResponseState currentRequest,
			EnquiryFormNotifications notificationsCfg, SqlSession sql)
			throws EngineException
	{
		if (notificationsCfg.getChannel() == null)
			return null;

		NotificationFacility notificationFacility = facilitiesManagement.getNotificationFacilityForChannel(
				notificationsCfg.getChannel(), sql);
		try
		{
			return notificationFacility.getAddressForEntity(
				new EntityParam(currentRequest.getEntityId()), sql, null);
		} catch (Exception e)
		{
			return notificationFacility.getAddressForUserRequest(currentRequest, sql);
		}
	}

	public EnquiryTranslationProfile getEnquiryProfileInstance(TranslationProfile profile)
	{
		return new EnquiryTranslationProfile(profile.getName(), profile.getRules(), 
				registrationTranslationActionsRegistry);
	}
}
