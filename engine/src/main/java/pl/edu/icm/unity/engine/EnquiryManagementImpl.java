/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.reg.EnquiryFormDB;
import pl.edu.icm.unity.db.generic.reg.EnquiryResponseDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.registration.BaseFormValidator;
import pl.edu.icm.unity.engine.registration.EnquiryResponseValidator;
import pl.edu.icm.unity.engine.registration.RegistrationConfirmationSupport;
import pl.edu.icm.unity.engine.registration.SharedEnquiryManagment;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
import pl.edu.icm.unity.server.api.registration.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.EnquiryFilledTemplateDef;
import pl.edu.icm.unity.server.api.registration.NewEnquiryTemplateDef;
import pl.edu.icm.unity.server.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.server.api.registration.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 * Implementation of the enquiry management API.
 * 
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class EnquiryManagementImpl implements EnquiryManagement
{
	private EnquiryFormDB enquiryFormDB;
	private EnquiryResponseDB requestDB;
	private NotificationProducer notificationProducer;
	private RegistrationConfirmationSupport confirmationsSupport;
	private UnityMessageSource msg;
	private AuthorizationManager authz;
	private BaseFormValidator baseFormValidator;
	private EnquiryResponseValidator enquiryResponseValidator;
	private SharedEndpointManagement sharedEndpointMan;
	private TransactionalRunner tx;
	private SharedEnquiryManagment internalManagment;
	
	@Autowired
	public EnquiryManagementImpl(EnquiryFormDB enquiryFormDB, EnquiryResponseDB requestDB,
			NotificationProducer notificationProducer,
			RegistrationConfirmationSupport confirmationsSupport,
			UnityMessageSource msg, AuthorizationManager authz,
			BaseFormValidator baseFormValidator,
			EnquiryResponseValidator enquiryResponseValidator,
			SharedEndpointManagement sharedEndpointMan, TransactionalRunner tx,
			SharedEnquiryManagment internalManagment)
	{
		this.enquiryFormDB = enquiryFormDB;
		this.requestDB = requestDB;
		this.notificationProducer = notificationProducer;
		this.confirmationsSupport = confirmationsSupport;
		this.msg = msg;
		this.authz = authz;
		this.baseFormValidator = baseFormValidator;
		this.enquiryResponseValidator = enquiryResponseValidator;
		this.sharedEndpointMan = sharedEndpointMan;
		this.tx = tx;
		this.internalManagment = internalManagment;
	}

	@Transactional
	@Override
	public void addEnquiry(EnquiryForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		validateFormContents(form, sql);
		enquiryFormDB.insert(form.getName(), form, sql);
	}
	
	@Transactional
	@Override
	public void sendEnquiry(String enquiryId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		EnquiryForm form = enquiryFormDB.get(enquiryId, SqlSessionTL.get());
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
	public void removeEnquiry(String formId, boolean dropRequests) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		internalManagment.removeForm(formId, dropRequests, requestDB, enquiryFormDB, SqlSessionTL.get());
	}
	
	@Transactional
	@Override
	public void updateEnquiry(EnquiryForm updatedForm, boolean ignoreRequests) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		validateFormContents(updatedForm, sql);
		String formId = updatedForm.getName();
		if (!ignoreRequests)
			internalManagment.validateIfHasPendingRequests(formId, requestDB, sql);
		enquiryFormDB.update(updatedForm.getName(), updatedForm, sql);
	}
	
	@Transactional
	@Override
	public List<EnquiryForm> getEnquires() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return enquiryFormDB.getAll(SqlSessionTL.get());
	}
	
	
	@Override
	public String submitEnquiryResponse(EnquiryResponse response, RegistrationContext context) throws EngineException
	{
		EnquiryResponseState responseFull = new EnquiryResponseState();
		responseFull.setStatus(RegistrationRequestStatus.pending);
		responseFull.setRequest(response);
		responseFull.setRequestId(UUID.randomUUID().toString());
		responseFull.setTimestamp(new Date());
		responseFull.setRegistrationContext(context);
		responseFull.setEntityId(InvocationContext.getCurrent().getLoginSession().getEntityId());
		
		EnquiryForm form = recordRequestAndReturnForm(responseFull);
		sendNotificationOnNewResponse(form, response);
		tryAutoProcess(form, responseFull, context);
		
		confirmationsSupport.sendAttributeConfirmationRequest(responseFull, form);
		confirmationsSupport.sendIdentityConfirmationRequest(responseFull, form);
		
		return responseFull.getRequestId();
	}
	
	
	@Override
	@Transactional
	public void processEnquiryResponse(String id, EnquiryResponse finalRequest,
			RegistrationRequestAction action, String publicCommentStr,
			String internalCommentStr) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.credentialModify, AuthzCapability.attributeModify,
				AuthzCapability.identityModify, AuthzCapability.groupModify);
		SqlSession sql = SqlSessionTL.get();
		EnquiryResponseState currentRequest = requestDB.get(id, sql);
		
		LoginSession client = internalManagment.preprocessRequest(finalRequest, currentRequest, action);
		
		AdminComment publicComment = internalManagment.preprocessComment(currentRequest, 
				publicCommentStr, client, true);
		AdminComment internalComment = internalManagment.preprocessComment(currentRequest, 
				internalCommentStr, client, false);

		EnquiryForm form = enquiryFormDB.get(currentRequest.getRequest().getFormId(), sql);

		switch (action)
		{
		case drop:
			internalManagment.dropEnquiryResponse(id, sql);
			break;
		case reject:
			internalManagment.rejectEnquiryResponse(form, currentRequest, publicComment, internalComment, sql);
			break;
		case update:
			updateResponse(form, currentRequest, publicComment, internalComment, sql);
			break;
		case accept:
			internalManagment.acceptEnquiryResponse(form, currentRequest, publicComment, 
					internalComment, true, sql);
			break;
		}
	}

	private void updateResponse(EnquiryForm form, EnquiryResponseState currentRequest,
			AdminComment publicComment, AdminComment internalComment, SqlSession sql) 
			throws EngineException
	{
		enquiryResponseValidator.validateSubmittedRequest(form, currentRequest.getRequest(), false, sql);
		requestDB.update(currentRequest.getRequestId(), currentRequest, sql);
	}
	
	private EnquiryForm recordRequestAndReturnForm(EnquiryResponseState responseFull) throws EngineException
	{
		return tx.runInTransactionRet(() -> {
			SqlSession sql = SqlSessionTL.get();
			EnquiryForm form = enquiryFormDB.get(responseFull.getRequest().getFormId(), sql);
			enquiryResponseValidator.validateSubmittedRequest(form, responseFull.getRequest(), true, sql);
			requestDB.insert(responseFull.getRequestId(), responseFull, sql);
			return form;
		});
	}

	private void sendNotificationOnNewResponse(EnquiryForm form, EnquiryResponse response) throws EngineException
	{
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		if (notificationsCfg.getChannel() != null && notificationsCfg.getSubmittedTemplate() != null
				&& notificationsCfg.getAdminsNotificationGroup() != null)
		{
			Map<String, String> params = new HashMap<>();
			params.put(EnquiryFilledTemplateDef.FORM_NAME, form.getDisplayedName().getDefaultValue());
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			params.put(EnquiryFilledTemplateDef.USER, loginSession.getEntityLabel());
			notificationProducer.sendNotificationToGroup(
					notificationsCfg.getAdminsNotificationGroup(), 
					notificationsCfg.getChannel(), 
					notificationsCfg.getSubmittedTemplate(),
					params,
					msg.getDefaultLocaleCode());
		}
	}
	
	private void tryAutoProcess(EnquiryForm form, EnquiryResponseState requestFull, 
			RegistrationContext context) throws EngineException
	{
		if (!context.tryAutoAccept)
			return;
		tx.runInTransaction(() -> {
			internalManagment.autoProcessEnquiry(form, requestFull, 
						"Automatic processing of the request  " + 
						requestFull.getRequestId() + " invoked, action: {0}", 
						SqlSessionTL.get());
		});
	}

	
	private void validateFormContents(EnquiryForm form, SqlSession sql) throws EngineException
	{
		baseFormValidator.validateBaseFormContents(form, sql);
		
		EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg == null)
			throw new WrongArgumentException("NotificationsConfiguration must be set in the form.");
		baseFormValidator.checkTemplate(notCfg.getSubmittedTemplate(), EnquiryFilledTemplateDef.NAME,
				sql, "enquiry filled");
		baseFormValidator.checkTemplate(notCfg.getEnquiryToFillTemplate(), NewEnquiryTemplateDef.NAME,
				sql, "new enquiry");
		baseFormValidator.checkTemplate(notCfg.getAcceptedTemplate(), AcceptRegistrationTemplateDef.NAME,
				sql, "enquiry accepted");
		baseFormValidator.checkTemplate(notCfg.getAcceptedTemplate(), RejectRegistrationTemplateDef.NAME,
				sql, "enquiry rejected");
		
		if (form.getTargetGroups() == null || form.getTargetGroups().length == 0)
			throw new WrongArgumentException("Target groups must be set in the form.");
	}
}
