/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.forms.BaseSharedRegistrationSupport;
import pl.edu.icm.unity.engine.forms.reg.RegistrationConfirmationRewriteSupport;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.engine.identity.IdentityHelper;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.engine.translation.form.EnquiryTranslationProfile;
import pl.edu.icm.unity.engine.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

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
	private IdentityHelper dbIdentities;
	private RegistrationConfirmationRewriteSupport confirmationsSupport;
	private InternalFacilitiesManagement facilitiesManagement;
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	private EnquiryResponseValidator responseValidator;
	
	@Autowired
	public SharedEnquiryManagment(UnityMessageSource msg,
			NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, GroupHelper groupHelper,
			EntityCredentialsHelper entityCredentialsHelper,
			EnquiryResponseDB enquiryResponseDB, IdentityHelper dbIdentities,
			RegistrationConfirmationRewriteSupport confirmationsSupport,
			InternalFacilitiesManagement facilitiesManagement,
			RegistrationActionsRegistry registrationTranslationActionsRegistry,
			EnquiryResponseValidator responseValidator)
	{
		super(msg, notificationProducer, attributesHelper, groupHelper,
				entityCredentialsHelper);
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
			boolean rewriteConfirmationToken) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.accepted);

		EnquiryTranslationProfile translationProfile = new EnquiryTranslationProfile(
				form.getTranslationProfile(), registrationTranslationActionsRegistry);
		TranslatedRegistrationRequest translatedRequest = translationProfile.translate(form, currentRequest);
		
		responseValidator.validateTranslatedRequest(form, currentRequest.getRequest(), 
				translatedRequest);
		enquiryResponseDB.update(currentRequest);

		List<Attribute> rootAttributes = new ArrayList<>(translatedRequest.getAttributes().size());
		Map<String, List<Attribute>> remainingAttributesByGroup = new HashMap<>();
		for (Attribute a : translatedRequest.getAttributes())
			addAttributeToGroupsMap(a, rootAttributes, remainingAttributesByGroup);

		long entityId = currentRequest.getEntityId();
		Collection<IdentityParam> identities = translatedRequest.getIdentities();
		Iterator<IdentityParam> identitiesIterator = identities.iterator();
		while (identitiesIterator.hasNext())
		{
			IdentityParam idParam = identitiesIterator.next();
			dbIdentities.insertIdentity(idParam, entityId, false);
		}

		attributesHelper.addAttributesList(rootAttributes, entityId, true);
		
		applyRequestedGroups(entityId, remainingAttributesByGroup, 
				translatedRequest);

		applyRequestedAttributeClasses(translatedRequest, entityId);
		
		applyRequestedCredentials(currentRequest, entityId);
		
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg);
		sendProcessingNotification(notificationsCfg.getAcceptedTemplate(), currentRequest,
				form.getName(), true, publicComment,
				internalComment, notificationsCfg, requesterAddress);
		if (rewriteConfirmationToken)
			confirmationsSupport.rewriteRequestToken(currentRequest, entityId);
	}
	
	public void dropEnquiryResponse(String id) throws EngineException
	{
		enquiryResponseDB.delete(id);
	}
	
	public void rejectEnquiryResponse(EnquiryForm form, EnquiryResponseState currentRequest, 
			AdminComment publicComment, AdminComment internalComment) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.rejected);
		enquiryResponseDB.update(currentRequest);
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg);
		sendProcessingNotification(notificationsCfg.getRejectedTemplate(), 
				currentRequest, form.getName(),
				true, publicComment, 
				internalComment, notificationsCfg, requesterAddress);
	}
	
	public void sendProcessingNotification(EnquiryForm form, String templateId,
			EnquiryResponseState currentRequest, String formId,
			AdminComment publicComment, AdminComment internalComment)
			throws EngineException
	{
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg);
		sendProcessingNotification(templateId, currentRequest, formId, false, 
				publicComment, internalComment, notificationsCfg, requesterAddress);
	}
	
	/**
	 * Basing on the profile's decision automatically process the enquiry response if needed.
	 * @throws EngineException 
	 * @return true only if request was accepted
	 */
	public boolean autoProcessEnquiry(EnquiryForm form, EnquiryResponseState fullResponse, 
			String logMessageTemplate) throws EngineException
	{
		EnquiryTranslationProfile translationProfile = new EnquiryTranslationProfile(
				form.getTranslationProfile(), registrationTranslationActionsRegistry);
		
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
			acceptEnquiryResponse(form, fullResponse, null, systemComment, false);
			return true;
		case drop:
			dropEnquiryResponse(fullResponse.getRequestId());
			break;
		case reject:
			fullResponse.getAdminComments().add(systemComment);
			rejectEnquiryResponse(form, fullResponse, null, systemComment);
			break;
		default:
		}
		return false;
	}
	
	private String getRequesterAddress(EnquiryResponseState currentRequest,
			EnquiryFormNotifications notificationsCfg)
			throws EngineException
	{
		if (notificationsCfg.getChannel() == null)
			return null;

		NotificationFacility notificationFacility = facilitiesManagement.getNotificationFacilityForChannel(
				notificationsCfg.getChannel());
		try
		{
			return notificationFacility.getAddressForEntity(
				new EntityParam(currentRequest.getEntityId()), null);
		} catch (Exception e)
		{
			return notificationFacility.getAddressForUserRequest(currentRequest);
		}
	}
}
