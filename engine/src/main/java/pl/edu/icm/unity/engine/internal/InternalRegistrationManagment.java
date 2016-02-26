/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqIdentityConfirmationState;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.api.registration.BaseRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.RegistrationWithCommentsTemplateDef;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.GroupParam;
import pl.edu.icm.unity.server.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Implementation of the internal registration management. This is used
 * internally and not exposed by the public interfaces.
 * 
 * @author P. Piernik
 */
@Component
public class InternalRegistrationManagment
{
	private static final Logger log = Log.getLogger(Log.U_SERVER,
			InternalRegistrationManagment.class);

	public static final String AUTO_PROCESS_COMMENT = "Automatically processed";

	@Autowired
	private RegistrationFormDB formsDB;
	@Autowired
	private RegistrationRequestDB requestDB;
	@Autowired
	private DBIdentities dbIdentities;
	@Autowired
	private DBGroups dbGroups;
	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private EngineHelper engineHelper;
	@Autowired
	private AttributesHelper attributesHelper;
	@Autowired
	private NotificationProducer notificationProducer;
	@Autowired
	private InternalFacilitiesManagement facilitiesManagement;
	@Autowired
	private RegistrationRequestValidator registrationRequestValidator;
	@Autowired
	private RegistrationActionsRegistry registrationTranslationActionsRegistry;


	public List<RegistrationForm> getForms(SqlSession sql) throws EngineException
	{
		return formsDB.getAll(sql);
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

		RegistrationTranslationProfile translationProfile = getProfileInstance(form.getTranslationProfile());
		TranslatedRegistrationRequest translatedRequest = translationProfile.translate(form, currentRequest);
		
		registrationRequestValidator.validateTranslatedRequest(form, currentRequest.getRequest(), 
				translatedRequest, sql);
		requestDB.update(currentRequest.getRequestId(), currentRequest, sql);

		List<Attribute<?>> rootAttributes = new ArrayList<>(translatedRequest.getAttributes().size());
		Map<String, List<Attribute<?>>> remainingAttributesByGroup = new HashMap<String, List<Attribute<?>>>();
		for (Attribute<?> a : translatedRequest.getAttributes())
			addAttr(a, rootAttributes, remainingAttributesByGroup);

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

		Map<String, GroupParam> sortedGroups = new TreeMap<>();
		for (GroupParam group : translatedRequest.getGroups())
			sortedGroups.put(group.getGroup(), group);

		EntityParam entity = new EntityParam(initial.getEntityId());
		for (Map.Entry<String, GroupParam> entry : sortedGroups.entrySet())
		{
			List<Attribute<?>> attributes = remainingAttributesByGroup.get(entry.getKey());
			if (attributes == null)
				attributes = Collections.emptyList();
			attributesHelper.checkGroupAttributeClassesConsistency(attributes, entry.getKey(), sql);
			GroupParam sel = entry.getValue();
			String idp = sel == null ? null : sel.getExternalIdp();
			String profile = sel == null ? null : sel.getTranslationProfile();
			dbGroups.addMemberFromParent(entry.getKey(), entity, idp, profile, new Date(), sql);
			attributesHelper.addAttributesList(attributes, initial.getEntityId(),
					true, sql);
		}

		Map<String, Set<String>> attributeClasses = translatedRequest.getAttributeClasses();
		for (Map.Entry<String, Set<String>> groupAcs: attributeClasses.entrySet())
		{
			attributesHelper.setAttributeClasses(initial.getEntityId(), groupAcs.getKey(), 
					groupAcs.getValue(), sql);
		}
		
		RegistrationRequest originalRequest = currentRequest.getRequest();
		if (originalRequest.getCredentials() != null)
		{
			for (CredentialParamValue c : originalRequest.getCredentials())
			{
				engineHelper.setPreviouslyPreparedEntityCredentialInternal(
						initial.getEntityId(), c.getSecrets(),
						c.getCredentialId(), sql);
			}
		}
		RegistrationFormNotifications notificationsCfg = form
				.getNotificationsConfiguration();
		sendProcessingNotification(notificationsCfg.getAcceptedTemplate(), currentRequest,
				currentRequest.getRequestId(), form.getName(), true, publicComment,
				internalComment, notificationsCfg, sql);
		if (rewriteConfirmationToken)
			rewriteRequestTokenInternal(currentRequest, initial.getEntityId());

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
		sendProcessingNotification(notificationsCfg.getRejectedTemplate(), 
				currentRequest, currentRequest.getRequestId(), form.getName(),
				true, publicComment, 
				internalComment, notificationsCfg, sql);
	}
	


	private void addAttr(Attribute<?> a, List<Attribute<?>> rootAttributes,
			Map<String, List<Attribute<?>>> remainingAttributesByGroup)
	{
		String path = a.getGroupPath();
		if (path.equals("/"))
			rootAttributes.add(a);
		else
		{
			List<Attribute<?>> attrs = remainingAttributesByGroup.get(path);
			if (attrs == null)
			{
				attrs = new ArrayList<>();
				remainingAttributesByGroup.put(path, attrs);
			}
			attrs.add(a);
		}
	}

	public RegistrationRequestState getRequest(String requestId, SqlSession sql) throws EngineException
	{
		return requestDB.get(requestId, sql);
	}


	public Map<String, String> getBaseNotificationParams(String formId, String requestId)
	{
		Map<String, String> ret = new HashMap<>();
		ret.put(BaseRegistrationTemplateDef.FORM_NAME, formId);
		ret.put(BaseRegistrationTemplateDef.REQUEST_ID, requestId);
		return ret;
	}

	/**
	 * Basing on the profile's decision automatically process the request if needed.
	 * @return entity id if automatic request acceptance was performed.
	 * @throws EngineException 
	 */
	public Long autoProcess(RegistrationForm form, RegistrationRequestState requestFull, String logMessageTemplate,
			SqlSession sql)	throws EngineException
	{
		RegistrationTranslationProfile translationProfile = getProfileInstance(form.getTranslationProfile());
		
		AutomaticRequestAction autoProcessAction = translationProfile.getAutoProcessAction(
				form, requestFull, RequestSubmitStatus.submitted);
		if (autoProcessAction == AutomaticRequestAction.none)
			return null;

		AdminComment systemComment = new AdminComment(
				InternalRegistrationManagment.AUTO_PROCESS_COMMENT, 0, false);

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
	 * Accepts the enquiry response, unless its form profile returns drop o reject action.
	 * In future this will be enhanced to the pipeline similar to registration processing. 
	 * @throws EngineException 
	 */
	public void processEnquiry(EnquiryForm form, EnquiryResponse response, String logMessageTemplate,
			SqlSession sql)	throws EngineException
	{
		RegistrationTranslationProfile translationProfile = getProfileInstance(form.getTranslationProfile());
		
		AutomaticRequestAction autoProcessAction = translationProfile.getAutoProcessAction(
				form, requestFull, RequestSubmitStatus.submitted);

		AdminComment systemComment = new AdminComment(
				InternalRegistrationManagment.AUTO_PROCESS_COMMENT, 0, false);

		String formattedMsg = MessageFormat.format(logMessageTemplate, autoProcessAction);
		log.info(formattedMsg);
		
		switch (autoProcessAction)
		{
		case none:
		case accept:
			return acceptRequest(form, requestFull, null, systemComment, false, sql);
		case drop:
		case reject:
		default:
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
			RegistrationRequestState currentRequest, String requestId, String formId,
			boolean sendToRequester, AdminComment publicComment,
			AdminComment internalComment,
			RegistrationFormNotifications notificationsCfg, SqlSession sql)
			throws EngineException
	{
		if (notificationsCfg.getChannel() == null || templateId == null)
			return;
		Map<String, String> notifyParams = getBaseNotificationParams(formId, requestId);
		notifyParams.put(RegistrationWithCommentsTemplateDef.PUBLIC_COMMENT,
				publicComment == null ? "" : publicComment.getContents());
		notifyParams.put(RegistrationWithCommentsTemplateDef.INTERNAL_COMMENT, "");
		String requesterAddress = getRequesterAddress(currentRequest, notificationsCfg, sql);
		if (requesterAddress != null)
		{
			if (sendToRequester || publicComment != null)
			{
				String userLocale = currentRequest.getRequest().getUserLocale();
				notificationProducer.sendNotification(requesterAddress,
						notificationsCfg.getChannel(), templateId,
						notifyParams, userLocale);
			}
		}

		if (notificationsCfg.getAdminsNotificationGroup() != null)
		{
			notifyParams.put(
					RegistrationWithCommentsTemplateDef.INTERNAL_COMMENT,
					internalComment == null ? "" : internalComment
							.getContents());
			notificationProducer.sendNotificationToGroup(
					notificationsCfg.getAdminsNotificationGroup(),
					notificationsCfg.getChannel(), templateId, notifyParams,
					msg.getDefaultLocaleCode());
		}
	}

	private String getRequesterAddress(RegistrationRequestState currentRequest,
			RegistrationFormNotifications notificationsCfg, SqlSession sql)
			throws EngineException
	{
		NotificationFacility notificationFacility = facilitiesManagement.getNotificationFacilityForChannel(
				notificationsCfg.getChannel(), sql);
		return notificationFacility.getAddressForRegistrationRequest(currentRequest, sql);
	}

	//TODO - needs version for enquiry rewrite
	private void rewriteRequestTokenInternal(RegistrationRequestState finalReguest, long entityId) 
			throws EngineException
	{

		List<Token> tks = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE);
		for (Token tk : tks)
		{
			RegistrationConfirmationState state;
			try
			{
				state = new RegistrationConfirmationState(tk.getContentsString());
			} catch (WrongArgumentException e)
			{
				//OK - not a registration token
				continue;
			}
			if (state.getRequestId().equals(finalReguest.getRequestId()))
			{
				if (state.getFacilityId().equals(
						RegistrationReqAttribiuteConfirmationState.FACILITY_ID))
				{
					rewriteSingleAttributeToken(finalReguest, tk, entityId);
				} else if (state.getFacilityId().equals(
						RegistrationReqIdentityConfirmationState.FACILITY_ID))
				{
					rewriteSingleIdentityToken(finalReguest, tk, entityId);
				}
			}
		}
	}

	private void rewriteSingleIdentityToken(RegistrationRequestState finalReguest, Token tk, 
			long entityId) throws EngineException
	{
		RegistrationReqIdentityConfirmationState oldState = new RegistrationReqIdentityConfirmationState(
				new String(tk.getContents(), StandardCharsets.UTF_8));
		boolean inRequest = false;
		for (IdentityParam id : finalReguest.getRequest().getIdentities())
		{
			if (id == null)
				continue;
			
			if (id.getTypeId().equals(oldState.getType())
					&& id.getValue().equals(oldState.getValue()))
			{
				inRequest = true;
				break;
			}
		}

		tokensMan.removeToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue());
		if (inRequest)
		{
			IdentityConfirmationState newstate = new IdentityConfirmationState(
					entityId, oldState.getType(), oldState.getValue(),
					oldState.getLocale(), oldState.getRedirectUrl());
			log.debug("Update confirmation token " + tk.getValue()
					+ " change facility to " + newstate.getFacilityId());
			tokensMan.addToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk
					.getValue(), newstate.getSerializedConfiguration()
					.getBytes(StandardCharsets.UTF_8), tk.getCreated(), tk
					.getExpires());
		}

	}

	private void rewriteSingleAttributeToken(RegistrationRequestState finalReguest, Token tk, 
			long entityId) throws EngineException
	{

		RegistrationReqAttribiuteConfirmationState oldState = new RegistrationReqAttribiuteConfirmationState(
				new String(tk.getContents(), StandardCharsets.UTF_8));
		boolean inRequest = false;
		for (Attribute<?> attribute : finalReguest.getRequest().getAttributes())
		{
			if (attribute == null || attribute.getAttributeSyntax() == null)
				continue;
			if (inRequest)
				break;
			
			if (attribute.getAttributeSyntax().isVerifiable()
					&& attribute.getName().equals(oldState.getType())
					&& attribute.getValues() != null)

			{
				for (Object o : attribute.getValues())
				{
					VerifiableElement val = (VerifiableElement) o;
					if (val.getValue().equals(oldState.getValue()))
					{
						inRequest = true;
						break;
					}
				}
			}
		}
		tokensMan.removeToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue());
		if (inRequest)
		{
			AttribiuteConfirmationState newstate = new AttribiuteConfirmationState(
					entityId, oldState.getType(), oldState.getValue(),
					oldState.getLocale(), oldState.getGroup(),
					oldState.getRedirectUrl());
			log.debug("Update confirmation token " + tk.getValue()
					+ " change facility to " + newstate.getFacilityId());
			tokensMan.addToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk
					.getValue(), newstate.getSerializedConfiguration()
					.getBytes(StandardCharsets.UTF_8), tk.getCreated(), tk
					.getExpires());
		}
	}
	
	private RegistrationTranslationProfile getProfileInstance(TranslationProfile profile)
	{
		return new RegistrationTranslationProfile(profile.getName(), profile.getRules(), 
				registrationTranslationActionsRegistry);
	}
}
