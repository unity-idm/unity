/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
import pl.edu.icm.unity.engine.forms.reg.RegistrationConfirmationRewriteSupport;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.engine.identity.IdentityHelper;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.engine.translation.form.EnquiryTranslationProfile;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 * Implementation of the shared code of enquires management. This class is used
 * by the main manager implementing the public API and other facilities which
 * trigger enquires processing.
 * 
 * @author P. Piernik
 */
@Component
public class SharedEnquiryManagment extends BaseSharedRegistrationSupport
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, SharedEnquiryManagment.class);

	private EnquiryResponseDB enquiryResponseDB;
	private IdentityHelper dbIdentities;
	private RegistrationConfirmationRewriteSupport confirmationsRewriteSupport;
	private RegistrationConfirmationSupport confirmationsSupport;
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;
	private EnquiryResponsePreprocessor responseValidator;
	private GroupDAO groupDB;
	
	private AttributeTypeHelper atHelper;

	@Autowired
	public SharedEnquiryManagment(UnityMessageSource msg, NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, GroupHelper groupHelper,
			EntityCredentialsHelper entityCredentialsHelper, EnquiryResponseDB enquiryResponseDB,
			IdentityHelper dbIdentities, RegistrationConfirmationRewriteSupport confirmationsRewriteSupport,
			InternalFacilitiesManagement facilitiesManagement,
			RegistrationActionsRegistry registrationTranslationActionsRegistry,
			EnquiryResponsePreprocessor responseValidator, AttributeTypeHelper atHelper,
			RegistrationConfirmationSupport confirmationsSupport, InvitationDB invitationDB, GroupDAO groupDB)
	{
		super(msg, notificationProducer, attributesHelper, groupHelper, entityCredentialsHelper,
				facilitiesManagement, invitationDB);
		this.enquiryResponseDB = enquiryResponseDB;
		this.dbIdentities = dbIdentities;
		this.confirmationsRewriteSupport = confirmationsRewriteSupport;
		this.registrationTranslationActionsRegistry = registrationTranslationActionsRegistry;
		this.responseValidator = responseValidator;
		this.atHelper = atHelper;
		this.confirmationsSupport = confirmationsSupport;
		this.groupDB = groupDB;
	}

	/**
	 * Accepts a enquiry response applying all enquiry form rules. The
	 * method operates on a result of the form's translation profile, rather
	 * then on the original request.
	 * 
	 * @param form
	 * @param currentRequest
	 * @param publicComment
	 * @param internalComment
	 * @param rewriteConfirmationToken
	 * @param sql
	 * @throws EngineException
	 */
	public void acceptEnquiryResponse(EnquiryForm form, EnquiryResponseState currentRequest,
			AdminComment publicComment, AdminComment internalComment, boolean rewriteConfirmationToken)
			throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.accepted);

		EnquiryTranslationProfile translationProfile = new EnquiryTranslationProfile(
				form.getTranslationProfile(), registrationTranslationActionsRegistry, atHelper, form);
		TranslatedRegistrationRequest translatedRequest = translationProfile.translate(currentRequest);

		responseValidator.validateTranslatedRequest(form, currentRequest.getRequest(), translatedRequest);
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

		List<Group> allUserGroups = groupHelper.getEntityGroups(entityId);
	
		applyGroupsAndAttributesFromEnquiry(entityId, groupDB.getAll(), allUserGroups, remainingAttributesByGroup,
				translatedRequest.getGroups(), form, currentRequest.getRequest());

	
		applyRequestedAttributeClasses(translatedRequest.getAttributeClasses(), entityId);

		applyRequestedCredentials(currentRequest, entityId);

		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		String templateId = notificationsCfg.getAcceptedTemplate();
		String requesterAddress = getRequesterAddress(currentRequest, templateId);
		sendProcessingNotification(templateId, currentRequest, form.getName(), true, publicComment,
				internalComment, notificationsCfg, requesterAddress);

		confirmationsSupport.sendAttributeConfirmationRequest(currentRequest, form, entityId, Phase.ON_ACCEPT);
		confirmationsSupport.sendIdentityConfirmationRequest(currentRequest, form, entityId, Phase.ON_ACCEPT);
		if (rewriteConfirmationToken)
			confirmationsRewriteSupport.rewriteRequestToken(currentRequest, entityId);
	}

	private void applyGroupsAndAttributesFromEnquiry(long entityId, List<Group> allGroups, List<Group> allUserGroups,
			Map<String, List<Attribute>> remainingAttributesByGroup, Collection<GroupParam> requestedGroup,
			EnquiryForm form, EnquiryResponse response) throws EngineException
	{

		RequestedGroupDiff diff = GroupDiffUtils.getAllRequestedGroupsDiff(allGroups, allUserGroups, response.getGroupSelections(),
				form.getGroupParams());

		List<GroupParam> toAdd = requestedGroup.stream()
				.filter(p -> diff.toAdd.contains(p.getGroup()))
				.collect(Collectors.toList());
		toAdd.addAll(requestedGroup.stream().filter(g -> g.getTranslationProfile() != null).collect(Collectors.toList()));
	
		applyRequestedGroups(entityId, remainingAttributesByGroup, toAdd, allUserGroups);
		if (form.getType().equals(EnquiryType.STICKY))
		{
			applyRemovedGroup(entityId, diff.toRemove);
		}

		applyRequestedAttributesInCurrentMembership(entityId, remainingAttributesByGroup, toAdd, allUserGroups);
	}
	
	/**
	 * Remove from group
	 * 
	 * @param entityId
	 * @param toRemove
	 */
	private void applyRemovedGroup(long entityId, Set<String> toRemove)
	{
		groupHelper.removeFromGroups(entityId, toRemove);
	}

	/**
	 * The group of the requested attributes may not have the "requested in
	 * the enquiry group" counterpart. User may already be a member of the
	 * group which is requested in attribute.
	 * 
	 * @param collection
	 */
	private void applyRequestedAttributesInCurrentMembership(long entityId,
			Map<String, List<Attribute>> remainingAttributesByGroup, Collection<GroupParam> requestedGroups, List<Group> actualGroups)
			throws EngineException
	{
		Set<String> groupsAlreadyProcessed = establishMissingGroups(requestedGroups, actualGroups);
		for (Map.Entry<String, List<Attribute>> entry : remainingAttributesByGroup.entrySet())
		{
			String attributeGroup = entry.getKey();
			if (!groupsAlreadyProcessed.contains(attributeGroup)
					&& groupHelper.isMember(entityId, attributeGroup))
			{
				List<Attribute> attributes = entry.getValue();
				attributesHelper.checkGroupAttributeClassesConsistency(attributes, attributeGroup);
				attributesHelper.addAttributesList(attributes, entityId, true);
			}
		}
	}

	/**
	 * @param requestedGroups
	 * @param actualGroups
	 * @return
	 */
	private Set<String> establishMissingGroups(Collection<GroupParam> requestedGroups, List<Group> actualGroups)
	{
		Set<String> allGroups = new HashSet<>();
		if (actualGroups != null && !actualGroups.isEmpty())
		{
			allGroups.addAll(actualGroups.stream().map(g -> g.toString()).collect(Collectors.toList()));
		} else
		{

			allGroups.add("/");
		}
		
		Set<String> missing = new HashSet<>();
		for (GroupParam group : requestedGroups)
		{
			Deque<String> missingGroups = Group.getMissingGroups(group.getGroup(), allGroups);
			missing.addAll(missingGroups);
		}
		return missing;
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
		String templateId = notificationsCfg.getRejectedTemplate();
		String requesterAddress = getRequesterAddress(currentRequest, templateId);
		sendProcessingNotification(templateId, currentRequest, form.getName(), true, publicComment,
				internalComment, notificationsCfg, requesterAddress);
	}

	public void sendProcessingNotification(EnquiryForm form, String templateId, EnquiryResponseState currentRequest,
			String formId, AdminComment publicComment, AdminComment internalComment) throws EngineException
	{
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		String requesterAddress = getRequesterAddress(currentRequest, templateId);
		sendProcessingNotification(templateId, currentRequest, formId, false, publicComment, internalComment,
				notificationsCfg, requesterAddress);
	}

	/**
	 * Basing on the profile's decision automatically process the enquiry
	 * response if needed.
	 * 
	 * @throws EngineException
	 * @return true only if request was accepted
	 */
	public boolean autoProcessEnquiry(EnquiryForm form, EnquiryResponseState fullResponse,
			String logMessageTemplate) throws EngineException
	{
		EnquiryTranslationProfile translationProfile = new EnquiryTranslationProfile(
				form.getTranslationProfile(), registrationTranslationActionsRegistry, atHelper, form);

		AutomaticRequestAction autoProcessAction = translationProfile.getAutoProcessAction(fullResponse,
				RequestSubmitStatus.submitted);
		if (autoProcessAction == AutomaticRequestAction.none)
			return false;

		AdminComment systemComment = new AdminComment(SharedEnquiryManagment.AUTO_PROCESS_COMMENT, 0, false);

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

	@EventListener
	public void onAutoProcessEvent(EnquiryResponseAutoProcessEvent event)
	{
		try
		{
			autoProcessEnquiry(event.form, event.requestFull, event.logMessageTemplate);
		} catch (EngineException e)
		{
			log.error("Auto processing of registration form in result of async event failed", e);
		}
	}

	private String getRequesterAddress(EnquiryResponseState currentRequest, String templateId)
			throws EngineException
	{

		if (templateId == null || templateId.isEmpty())
			return null;

		NotificationFacility notificationFacility = facilitiesManagement
				.getNotificationFacilityForMessageTemplate(templateId);
		if (notificationFacility == null)
			return null;
		try
		{
			return notificationFacility.getAddressForEntity(new EntityParam(currentRequest.getEntityId()),
					null, false);
		} catch (Exception e)
		{
			return notificationFacility.getAddressForUserRequest(currentRequest);
		}
	}
}
