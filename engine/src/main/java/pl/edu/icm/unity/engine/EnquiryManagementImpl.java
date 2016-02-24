/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.reg.EnquiryFormDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.internal.BaseFormValidator;
import pl.edu.icm.unity.engine.internal.EnquiryResponseValidator;
import pl.edu.icm.unity.engine.internal.InternalRegistrationManagment;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
import pl.edu.icm.unity.server.api.registration.EnquiryFilledTemplateDef;
import pl.edu.icm.unity.server.api.registration.NewEnquiryTemplateDef;
import pl.edu.icm.unity.server.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.translation.form.EnquiryTranslationProfile;
import pl.edu.icm.unity.server.translation.form.GroupParam;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Implementation of the enquiry management API.
 * 
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class EnquiryManagementImpl implements EnquiryManagement
{
	private EnquiryFormDB enquiryDB;
	private NotificationProducer notificationProducer;
	private UnityMessageSource msg;
	private AuthorizationManager authz;
	private BaseFormValidator baseFormValidator;
	private EnquiryResponseValidator enquiryResponseValidator;
	private SharedEndpointManagement sharedEndpointMan;
	private TransactionalRunner tx;
	
	@Transactional
	@Override
	public void addEnquiry(EnquiryForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		validateFormContents(form, sql);
		enquiryDB.insert(form.getName(), form, sql);
	}
	
	@Transactional
	@Override
	public void sendEnquiry(String enquiryId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		EnquiryForm form = enquiryDB.get(enquiryId, SqlSessionTL.get());
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		
		if (notificationsCfg.getChannel() != null && notificationsCfg.getEnquiryToFillTemplate() != null)
		{

			Map<String, String> params = new HashMap<>();
			params.put(NewEnquiryTemplateDef.FORM_NAME, form.getDisplayedName().getDefaultValue());
			params.put(NewEnquiryTemplateDef.URL, 
					PublicRegistrationURLSupport.getPublicEnquiryLink(enquiryId, sharedEndpointMan));
			
			for (String group: form.getTargetGroups())
				notificationProducer.sendNotificationToGroup(
					group, 
					notificationsCfg.getChannel(), 
					notificationsCfg.getEnquiryToFillTemplate(),
					params,
					msg.getDefaultLocaleCode());
		}
	}
	
	@Transactional
	@Override
	public void removeEnquiry(String formId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		enquiryDB.remove(formId, SqlSessionTL.get());
	}
	
	@Transactional
	@Override
	public void updateEnquiry(EnquiryForm updatedForm) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		validateFormContents(updatedForm, sql);
		enquiryDB.update(updatedForm.getName(), updatedForm, sql);
	}
	
	@Transactional
	@Override
	public List<EnquiryForm> getEnquires() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return enquiryDB.getAll(SqlSessionTL.get());
	}
	
	
	@Override
	public void submitEnquiryResponse(EnquiryResponse response) throws EngineException
	{
		// TODO Auto-generated method stub
		EnquiryForm form = validateResponse(response);
		sendNotificationOnNewResponse(form, response);
		
	}
	
	private EnquiryForm validateResponse(EnquiryResponse response) throws EngineException
	{
		return tx.runInTransactionRet(() -> {
			SqlSession sql = SqlSessionTL.get();
			EnquiryForm form = enquiryDB.get(response.getFormId(), sql);
			enquiryResponseValidator.validateSubmittedRequest(form, response, true, sql);
			return form;
		});
	}

	private void sendNotificationOnNewResponse(EnquiryForm form, EnquiryResponse response) throws EngineException
	{
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		if (notificationsCfg.getChannel() != null && notificationsCfg.getEnquiryFilledTemplate() != null
				&& notificationsCfg.getAdminsNotificationGroup() != null)
		{
			Map<String, String> params = new HashMap<>();
			params.put(EnquiryFilledTemplateDef.FORM_NAME, form.getDisplayedName().getDefaultValue());
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			params.put(EnquiryFilledTemplateDef.USER, loginSession.getEntityLabel());
			notificationProducer.sendNotificationToGroup(
					notificationsCfg.getAdminsNotificationGroup(), 
					notificationsCfg.getChannel(), 
					notificationsCfg.getEnquiryFilledTemplate(),
					params,
					msg.getDefaultLocaleCode());
		}
	}
	
	private Long tryAutoProcess(RegistrationForm form, UserRequestState requestFull) throws EngineException
	{
		return tx.runInTransactionRet(() -> {
			return internalManagment.autoProcess(form, requestFull, 
						"Automatic processing of the request  " + 
						requestFull.getRequestId() + " invoked, action: {0}", 
						SqlSessionTL.get());
		});
	}

	/**
	 * Process the request: unless auto action returned by the profile is drop or reject, then 
	 * the request is accepted.
	 * @throws EngineException 
	 */
	public void autoProcess(RegistrationForm form, EnquiryResponseState requestFull, String logMessageTemplate,
			SqlSession sql)	throws EngineException
	{
		EnquiryTranslationProfile translationProfile = getProfileInstance(form.getTranslationProfile());
		
		AutomaticRequestAction autoProcessAction = translationProfile.getAutoProcessAction(
				form, requestFull, RequestSubmitStatus.submitted);
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
		case reject:
		default:
		}
	}
	
	private void validateFormContents(EnquiryForm form, SqlSession sql) throws EngineException
	{
		baseFormValidator.validateBaseFormContents(form, sql);
		
		EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg == null)
			throw new WrongArgumentException("NotificationsConfiguration must be set in the form.");
		baseFormValidator.checkTemplate(notCfg.getEnquiryFilledTemplate(), EnquiryFilledTemplateDef.NAME,
				sql, "enquiry filled");
		baseFormValidator.checkTemplate(notCfg.getEnquiryToFillTemplate(), NewEnquiryTemplateDef.NAME,
				sql, "new enquiry");
		
		if (form.getTargetGroups() == null || form.getTargetGroups().length == 0)
			throw new WrongArgumentException("Target groups must be set in the form.");
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
	public void acceptResponse(EnquiryForm form, EnquiryResponseState currentRequest,
			AdminComment publicComment, AdminComment internalComment,
			SqlSession sql) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.accepted);

		EnquiryTranslationProfile translationProfile = getProfileInstance(form.getTranslationProfile());
		TranslatedRegistrationRequest translatedRequest = translationProfile.translate(form, currentRequest);
		
		enquiryResponseValidator.validateTranslatedRequest(form, currentRequest.getRequest(), 
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
	
	
	private EnquiryTranslationProfile getProfileInstance(TranslationProfile profile)
	{
		return new EnquiryTranslationProfile(profile.getName(), profile.getRules(), 
				registrationTranslationActionsRegistry);
	}

}
