/**
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.credential.CredentialReqRepository;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.forms.BaseFormValidator;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport.Phase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;

/**
 * Implementation of registrations subsystem.
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class RegistrationsManagementImpl implements RegistrationsManagement
{
	private RegistrationFormDB formsDB;
	private RegistrationRequestDB requestDB;
	private CredentialReqRepository credentialReqRepository;
	private RegistrationConfirmationSupport confirmationsSupport;
	private AuthorizationManager authz;
	private NotificationProducer notificationProducer;

	private SharedRegistrationManagment internalManagment;
	private UnityMessageSource msg;
	private TransactionalRunner tx;
	private RegistrationRequestPreprocessor registrationRequestValidator;
	private BaseFormValidator baseValidator;

	@Autowired
	public RegistrationsManagementImpl(RegistrationFormDB formsDB,
			RegistrationRequestDB requestDB, CredentialReqRepository credentialReqDB,
			RegistrationConfirmationSupport confirmationsSupport,
			AuthorizationManager authz, NotificationProducer notificationProducer,
			SharedRegistrationManagment internalManagment, UnityMessageSource msg,
			TransactionalRunner tx,
			RegistrationRequestPreprocessor registrationRequestValidator,
			BaseFormValidator baseValidator)
	{
		this.formsDB = formsDB;
		this.requestDB = requestDB;
		this.credentialReqRepository = credentialReqDB;
		this.confirmationsSupport = confirmationsSupport;
		this.authz = authz;
		this.notificationProducer = notificationProducer;
		this.internalManagment = internalManagment;
		this.msg = msg;
		this.tx = tx;
		this.registrationRequestValidator = registrationRequestValidator;
		this.baseValidator = baseValidator;
	}

	@Override
	@Transactional
	public void addForm(RegistrationForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateFormContents(form);
		formsDB.create(form);
	}

	@Override
	@Transactional
	public void removeForm(String formId, boolean dropRequests) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		internalManagment.removeForm(formId, dropRequests, requestDB, formsDB);
	}

	@Override
	@Transactional
	public void updateForm(RegistrationForm updatedForm, boolean ignoreRequestsAndInvitations)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateFormContents(updatedForm);
		String formId = updatedForm.getName();
		if (!ignoreRequestsAndInvitations)
		{
			internalManagment.validateIfHasPendingRequests(formId, requestDB);
			internalManagment.validateIfHasInvitations(formId, InvitationType.REGISTRATION);
		}
		formsDB.update(updatedForm);
	}

	@Override
	@Transactional
	public List<RegistrationForm> getForms() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return formsDB.getAll();
	}

	@Override
	public String submitRegistrationRequest(RegistrationRequest request, final RegistrationContext context) 
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		RegistrationRequestState requestFull = new RegistrationRequestState();
		requestFull.setStatus(RegistrationRequestStatus.pending);
		requestFull.setRequest(request);
		requestFull.setRequestId(UUID.randomUUID().toString());
		requestFull.setTimestamp(new Date());
		requestFull.setRegistrationContext(context);
		
		RegistrationForm form = recordRequestAndReturnForm(requestFull); 
		
		sendNotification(form, requestFull);
		
		
		Long entityId = tryAutoProcess(form, requestFull, context);
		
		tx.runInTransactionThrowing(() -> {
			confirmationsSupport.sendAttributeConfirmationRequest(requestFull, entityId, form,
					Phase.ON_SUBMIT);
			confirmationsSupport.sendIdentityConfirmationRequest(requestFull, entityId, form,
					Phase.ON_SUBMIT);	
		});
		
		return requestFull.getRequestId();
	}

	private RegistrationForm recordRequestAndReturnForm(RegistrationRequestState requestFull) 
			throws EngineException
	{
		return tx.runInTransactionRetThrowing(() -> {
			RegistrationRequest request = requestFull.getRequest();
			RegistrationForm form = formsDB.get(request.getFormId());
			if (isCredentialsValidationSkipped(requestFull.getRegistrationContext().triggeringMode))
				registrationRequestValidator.validateSubmittedRequestExceptCredentials(form, request, true);
			else
				registrationRequestValidator.validateSubmittedRequest(form, request, true);
			requestDB.create(requestFull);
			return form;
		});
	}
	
	/**
	 * When user enters the registration form after selecting an option of
	 * remote authentication to fill out a form, the credentials are filtered
	 * out by default and not available to the user. This behavior is fixed, 
	 * meaning no configuration option to control this.
	 */
	private boolean isCredentialsValidationSkipped(TriggeringMode mode)
	{
		return mode == TriggeringMode.afterRemoteLoginFromRegistrationForm;
	}

	private void sendNotification(RegistrationForm form, RegistrationRequestState requestFull) 
			throws EngineException
	{
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		if (notificationsCfg.getSubmittedTemplate() != null
				&& notificationsCfg.getAdminsNotificationGroup() != null)
		{
			Map<String, String> params = internalManagment.getBaseNotificationParams(
					form.getName(), requestFull.getRequestId()); 
			notificationProducer.sendNotificationToGroup(
					notificationsCfg.getAdminsNotificationGroup(), 
					notificationsCfg.getSubmittedTemplate(),
					params,
					msg.getDefaultLocaleCode());
		}
	}
	
	private Long tryAutoProcess(RegistrationForm form, RegistrationRequestState requestFull, 
			RegistrationContext context) throws EngineException
	{
		return tx.runInTransactionRetThrowing(() -> {
			return internalManagment.autoProcess(form, requestFull, 
						"Automatic processing of the request  " + 
						requestFull.getRequestId() + " invoked, action: {0}");
		});
	}
	
	@Override
	@Transactional
	public List<RegistrationRequestState> getRegistrationRequests() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.read);
		return requestDB.getAll();
	}


	@Override
	@Transactional
	public RegistrationRequestState getRegistrationRequest(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.read);
		return requestDB.get(id);
	}

	@Override
	@Transactional
	public boolean hasForm(String id)
	{
		authz.checkAuthorizationRT("/", AuthzCapability.read);
		return formsDB.exists(id);
	}
	
	@Override
	@Transactional
	public void processRegistrationRequest(String id, RegistrationRequest finalRequest,
			RegistrationRequestAction action, String publicCommentStr,
			String internalCommentStr) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.credentialModify, AuthzCapability.attributeModify,
				AuthzCapability.identityModify, AuthzCapability.groupModify);
		RegistrationRequestState currentRequest = requestDB.get(id);

		LoginSession client = internalManagment.preprocessRequest(finalRequest, currentRequest, action);

		AdminComment publicComment = internalManagment.preprocessComment(currentRequest, 
				publicCommentStr, client, true);
		AdminComment internalComment = internalManagment.preprocessComment(currentRequest, 
				internalCommentStr, client, false);

		RegistrationForm form = formsDB.get(currentRequest.getRequest().getFormId());

		switch (action)
		{
		case drop:
			internalManagment.dropRequest(id);
			break;
		case reject:
			internalManagment.rejectRequest(form, currentRequest, publicComment, internalComment);
			break;
		case update:
			updateRequest(form, currentRequest, publicComment, internalComment);
			break;
		case accept:
			internalManagment.acceptRequest(form, currentRequest, publicComment, 
					internalComment, true);
			break;
		}
	}

	private void updateRequest(RegistrationForm form, RegistrationRequestState currentRequest,
			AdminComment publicComment, AdminComment internalComment) 
			throws EngineException
	{
		registrationRequestValidator.validateSubmittedRequest(form, currentRequest.getRequest(), false);
		requestDB.update(currentRequest);
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		internalManagment.sendProcessingNotification(notificationsCfg.getUpdatedTemplate(),
				currentRequest, form.getName(), false, 
				publicComment, internalComment,	notificationsCfg);
	}
	
	private void validateFormContents(RegistrationForm form) throws EngineException
	{
		baseValidator.validateBaseFormContents(form);
		
		if (form.isByInvitationOnly())
		{
			if (!form.isPubliclyAvailable())
				throw new IllegalArgumentException("Registration form which "
						+ "is by invitation only must be public");
			if (form.getRegistrationCode() != null)
				throw new IllegalArgumentException("Registration form which "
						+ "is by invitation only must not have a static registration code");
		}
		
		if (form.getDefaultCredentialRequirement() == null)
			throw new IllegalArgumentException("Credential requirement must be set for the form");
		if (credentialReqRepository.get(form.getDefaultCredentialRequirement()) == null)
			throw new IllegalArgumentException("Credential requirement " + 
					form.getDefaultCredentialRequirement() + " does not exist");

		RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg == null)
			throw new IllegalArgumentException("NotificationsConfiguration must be set in the form.");
		baseValidator.checkTemplate(notCfg.getAcceptedTemplate(), AcceptRegistrationTemplateDef.NAME,
				"accepted registration request");
		baseValidator.checkTemplate(notCfg.getRejectedTemplate(), RejectRegistrationTemplateDef.NAME,
				"rejected registration request");
		baseValidator.checkTemplate(notCfg.getSubmittedTemplate(), SubmitRegistrationTemplateDef.NAME,
				"submitted registration request");
		baseValidator.checkTemplate(notCfg.getUpdatedTemplate(), UpdateRegistrationTemplateDef.NAME,
				"updated registration request");
		baseValidator.checkTemplate(notCfg.getInvitationTemplate(), InvitationTemplateDef.NAME,
				"invitation");
		if (form.getCaptchaLength() > RegistrationForm.MAX_CAPTCHA_LENGTH)
			throw new IllegalArgumentException("Captcha can not be longer then " + 
					RegistrationForm.MAX_CAPTCHA_LENGTH + " characters");
	}

	@Override
	@Transactional
	public FormAutomationSupport getFormAutomationSupport(RegistrationForm form)
	{
		return confirmationsSupport.getRegistrationFormAutomationSupport(form);
	}

	@Override
	@Transactional
	public RegistrationForm getForm(String id) throws EngineException
	{
		return formsDB.get(id);
	}
}
