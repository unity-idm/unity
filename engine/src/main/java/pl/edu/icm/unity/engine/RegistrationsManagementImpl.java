/**
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqIdentityConfirmationState;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.db.generic.credreq.CredentialRequirementDB;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
import pl.edu.icm.unity.db.generic.reg.InvitationWithCodeDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.internal.InternalRegistrationManagment;
import pl.edu.icm.unity.engine.internal.RegistrationRequestValidator;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.api.RegistrationContext;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
import pl.edu.icm.unity.server.api.registration.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.BaseRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

/**
 * Implementation of registrations subsystem.
 * 
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class RegistrationsManagementImpl implements RegistrationsManagement
{
	private RegistrationFormDB formsDB;
	private RegistrationRequestDB requestDB;
	private CredentialDB credentialDB;
	private CredentialRequirementDB credentialReqDB;
	private DBAttributes dbAttributes;
	private MessageTemplateDB msgTplDB;
	private GroupResolver groupsResolver;
	private IdentityTypesRegistry identityTypesRegistry;
	private AuthorizationManager authz;
	private NotificationProducer notificationProducer;
	private ConfirmationManager confirmationManager;
	private InternalRegistrationManagment internalManagment;
	private UnityMessageSource msg;
	private TransactionalRunner tx;
	private RegistrationRequestValidator registrationRequestValidator;
	private InvitationWithCodeDB invitationDB;

	@Autowired
	public RegistrationsManagementImpl(RegistrationFormDB formsDB,
			RegistrationRequestDB requestDB, CredentialDB credentialDB,
			CredentialRequirementDB credentialReqDB, DBAttributes dbAttributes,
			MessageTemplateDB msgTplDB, GroupResolver groupsResolver,
			IdentityTypesRegistry identityTypesRegistry, AuthorizationManager authz,
			NotificationProducer notificationProducer,
			ConfirmationManager confirmationManager,
			InternalRegistrationManagment internalManagment, UnityMessageSource msg,
			TransactionalRunner tx,
			RegistrationRequestValidator registrationRequestValidator,
			InvitationWithCodeDB invitationDB)
	{
		this.formsDB = formsDB;
		this.requestDB = requestDB;
		this.credentialDB = credentialDB;
		this.credentialReqDB = credentialReqDB;
		this.dbAttributes = dbAttributes;
		this.msgTplDB = msgTplDB;
		this.groupsResolver = groupsResolver;
		this.identityTypesRegistry = identityTypesRegistry;
		this.authz = authz;
		this.notificationProducer = notificationProducer;
		this.confirmationManager = confirmationManager;
		this.internalManagment = internalManagment;
		this.msg = msg;
		this.tx = tx;
		this.registrationRequestValidator = registrationRequestValidator;
		this.invitationDB = invitationDB;
	}

	@Override
	@Transactional
	public void addForm(RegistrationForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		validateFormContents(form, sql);
		formsDB.insert(form.getName(), form, sql);
	}

	@Override
	@Transactional
	public void removeForm(String formId, boolean dropRequests) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		List<RegistrationRequestState> requests = requestDB.getAll(sql);
		if (dropRequests)
		{
			for (RegistrationRequestState req: requests)
				if (formId.equals(req.getRequest().getFormId()))
					requestDB.remove(req.getRequestId(), sql);
		} else
		{
			for (RegistrationRequestState req: requests)
				if (formId.equals(req.getRequest().getFormId()))
					throw new SchemaConsistencyException("There are requests bound " +
							"to this form, and it was not chosen to drop them.");
		}
		formsDB.remove(formId, sql);
	}

	@Override
	@Transactional
	public void updateForm(RegistrationForm updatedForm, boolean ignoreRequests)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		validateFormContents(updatedForm, sql);
		String formId = updatedForm.getName();
		if (!ignoreRequests)
		{
			List<RegistrationRequestState> requests = requestDB.getAll(sql);
			for (RegistrationRequestState req: requests)
				if (formId.equals(req.getRequest().getFormId()) && 
						req.getStatus() == RegistrationRequestStatus.pending)
					throw new SchemaConsistencyException("There are requests bound to " +
							"this form, and it was not chosen to ignore them.");
		}
		formsDB.update(formId, updatedForm, sql);
	}

	@Override
	@Transactional(noTransaction=true)
	public List<RegistrationForm> getForms() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sql = SqlSessionTL.get();
		return internalManagment.getForms(sql);
	}

	@Override
	public String submitRegistrationRequest(RegistrationRequest request, final RegistrationContext context) 
			throws EngineException
	{
		RegistrationRequestState requestFull = new RegistrationRequestState();
		requestFull.setStatus(RegistrationRequestStatus.pending);
		requestFull.setRequest(request);
		requestFull.setRequestId(UUID.randomUUID().toString());
		requestFull.setTimestamp(new Date());
		requestFull.setRegistrationContext(context);
		
		RegistrationForm form = recordRequestAndReturnForm(requestFull); 
			
		Long entityId = sendNotificationAndTryAutoProcess(form, requestFull, context);
		
		if (entityId == null)
			sendFormAttributeConfirmationRequest(requestFull, form);
		else
			sendAttributeConfirmationRequest(requestFull, entityId, form);
		sendIdentityConfirmationRequest(requestFull, entityId, form);	
		
		return requestFull.getRequestId();
	}

	private RegistrationForm recordRequestAndReturnForm(RegistrationRequestState requestFull) throws EngineException
	{
		return tx.runInTransacitonRet(() -> {
			RegistrationRequest request = requestFull.getRequest();
			SqlSession sql = SqlSessionTL.get();
			RegistrationForm form = formsDB.get(request.getFormId(), sql);
			registrationRequestValidator.validateSubmittedRequest(form, request, true, sql);
			requestDB.insert(requestFull.getRequestId(), requestFull, sql);
			return form;
		});
	}
	
	private Long sendNotificationAndTryAutoProcess(RegistrationForm form, RegistrationRequestState requestFull, 
			RegistrationContext context) throws EngineException
	{
		return tx.runInTransacitonRet(() -> {
			SqlSession sql = SqlSessionTL.get();
			Long entityId = null;
			RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
			if (notificationsCfg.getChannel() != null && notificationsCfg.getSubmittedTemplate() != null
					&& notificationsCfg.getAdminsNotificationGroup() != null)
			{
				Map<String, String> params = internalManagment.getBaseNotificationParams(
						form.getName(), requestFull.getRequestId()); 
				notificationProducer.sendNotificationToGroup(
						notificationsCfg.getAdminsNotificationGroup(), 
						notificationsCfg.getChannel(), 
						notificationsCfg.getSubmittedTemplate(),
						params,
						msg.getDefaultLocaleCode());
			}
			if (context.tryAutoAccept)
				entityId = internalManagment.autoProcess(form, requestFull, 
						"Automatic processing of the request  " + 
						requestFull.getRequestId() + " invoked, action: {0}", sql);
			
			return entityId;
		});
	}
	
	@Override
	@Transactional
	public List<RegistrationRequestState> getRegistrationRequests() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.read);
		return requestDB.getAll(SqlSessionTL.get());
	}

	@Override
	@Transactional
	public void processRegistrationRequest(String id, RegistrationRequest finalRequest,
			RegistrationRequestAction action, String publicCommentStr,
			String internalCommentStr) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.credentialModify, AuthzCapability.attributeModify,
				AuthzCapability.identityModify, AuthzCapability.groupModify);
		SqlSession sql = SqlSessionTL.get();
		RegistrationRequestState currentRequest = requestDB.get(id, sql);
		if (finalRequest != null)
		{
			finalRequest.setCredentials(currentRequest.getRequest().getCredentials());
			currentRequest.setRequest(finalRequest);
		}
		InvocationContext authnCtx = InvocationContext.getCurrent();
		LoginSession client = authnCtx.getLoginSession();

		if (client == null)
		{
			client = new LoginSession();
			client.setEntityId(0);
		}

		AdminComment publicComment = null;
		AdminComment internalComment = null;
		if (publicCommentStr != null)
		{
			publicComment = new AdminComment(publicCommentStr, client.getEntityId(), true);
			currentRequest.getAdminComments().add(publicComment);
		}
		if (internalCommentStr != null)
		{
			internalComment = new AdminComment(internalCommentStr, client.getEntityId(), false);
			currentRequest.getAdminComments().add(internalComment);
		}

		if (currentRequest.getStatus() != RegistrationRequestStatus.pending && 
				(action == RegistrationRequestAction.accept || 
				action == RegistrationRequestAction.reject))
			throw new WrongArgumentException("The request was already processed. " +
					"It is only possible to drop it or to modify its comments.");
		if (currentRequest.getStatus() != RegistrationRequestStatus.pending && 
				action == RegistrationRequestAction.update && finalRequest != null)
			throw new WrongArgumentException("The request was already processed. " +
					"It is only possible to drop it or to modify its comments.");
		RegistrationForm form = formsDB.get(currentRequest.getRequest().getFormId(), sql);

		switch (action)
		{
		case drop:
			internalManagment.dropRequest(id, sql);
			break;
		case reject:
			internalManagment.rejectRequest(form, currentRequest, publicComment, internalComment, sql);
			break;
		case update:
			updateRequest(form, currentRequest, publicComment, internalComment, sql);
			break;
		case accept:
			internalManagment.acceptRequest(form, currentRequest, publicComment, 
					internalComment, true, sql);
			break;
		}
	}

	private void updateRequest(RegistrationForm form, RegistrationRequestState currentRequest,
			AdminComment publicComment, AdminComment internalComment, SqlSession sql) 
			throws EngineException
	{
		registrationRequestValidator.validateSubmittedRequest(form, currentRequest.getRequest(), false, sql);
		requestDB.update(currentRequest.getRequestId(), currentRequest, sql);
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		internalManagment.sendProcessingNotification(notificationsCfg.getUpdatedTemplate(),
				currentRequest, currentRequest.getRequestId(), form.getName(), false, 
				publicComment, internalComment,	notificationsCfg, sql);
	}
	
	
	

	private void validateFormContents(RegistrationForm form, SqlSession sql) throws EngineException
	{
		GroupsMapper gm = sql.getMapper(GroupsMapper.class);

		Map<String, AttributeType> atMap = dbAttributes.getAttributeTypes(sql);

		if (form.getTranslationProfile() == null)
			throw new WrongArgumentException("Translation profile is not set.");
		
		if (form.getAttributeParams() != null)
		{
			Set<String> used = new HashSet<>();
			for (AttributeRegistrationParam attr: form.getAttributeParams())
			{
				if (!atMap.containsKey(attr.getAttributeType()))
					throw new WrongArgumentException("Attribute type " + attr.getAttributeType() + 
							" does not exist");
				String key = attr.getAttributeType() + " @ " + attr.getGroup();
				if (used.contains(key))
					throw new WrongArgumentException("Collected attribute " + key + 
							" was specified more then once.");
				used.add(key);
				groupsResolver.resolveGroup(attr.getGroup(), gm);
			}
		}

		if (form.getCredentialParams() != null)
		{
			Set<String> creds = new HashSet<>();
			for (CredentialRegistrationParam cred: form.getCredentialParams())
			{
				if (creds.contains(cred.getCredentialName()))
					throw new WrongArgumentException("Collected credential " + 
							cred.getCredentialName() + " was specified more then once.");
				creds.add(cred.getCredentialName());
			}
			credentialDB.assertExist(creds, sql);
		}

		if (form.getDefaultCredentialRequirement() == null)
			throw new WrongArgumentException("Credential requirement must be set for the form");
		if (credentialReqDB.get(form.getDefaultCredentialRequirement(), sql) == null)
			throw new WrongArgumentException("Credential requirement " + 
					form.getDefaultCredentialRequirement() + " does not exist");

		if (form.getGroupParams() != null)
		{
			Set<String> used = new HashSet<>();
			for (GroupRegistrationParam group: form.getGroupParams())
			{
				groupsResolver.resolveGroup(group.getGroupPath(), gm);
				if (used.contains(group.getGroupPath()))
					throw new WrongArgumentException("Selectable group " + group.getGroupPath() + 
							" was specified more then once.");
				used.add(group.getGroupPath());
			}
		}

		boolean hasIdentity = false;
		if (form.getIdentityParams() != null)
		{
			Set<String> usedRemote = new HashSet<>();
			for (IdentityRegistrationParam id: form.getIdentityParams())
			{
				identityTypesRegistry.getByName(id.getIdentityType());
				if (id.getRetrievalSettings() == ParameterRetrievalSettings.automatic || 
						id.getRetrievalSettings() == ParameterRetrievalSettings.automaticHidden)
				{
					if (usedRemote.contains(id.getIdentityType()))
						throw new WrongArgumentException("There can be only one identity " +
								"collected automatically of each type. There are more " +
								"then one of type " + id.getIdentityType());
					usedRemote.add(id.getIdentityType());
				}
				hasIdentity = true;
			}
		}
		if (!hasIdentity)
			throw new WrongArgumentException("Registration form must collect at least one identity.");
		
		RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg == null)
			throw new WrongArgumentException("NotificationsConfiguration must be set in the form.");
		checkTemplate(notCfg.getAcceptedTemplate(), AcceptRegistrationTemplateDef.NAME,
				sql, "accepted registration request");
		checkTemplate(notCfg.getRejectedTemplate(), RejectRegistrationTemplateDef.NAME,
				sql, "rejected registration request");
		checkTemplate(notCfg.getSubmittedTemplate(), SubmitRegistrationTemplateDef.NAME,
				sql, "submitted registration request");
		checkTemplate(notCfg.getUpdatedTemplate(), UpdateRegistrationTemplateDef.NAME,
				sql, "updated registration request");

		if (form.getAgreements() != null)
		{
			for (AgreementRegistrationParam o: form.getAgreements())
			{
				if (o.getText() == null || o.getText().isEmpty())
					throw new WrongArgumentException("Agreement text must not be empty.");
			}
		}
	}
	
	private void checkTemplate(String tpl, String compatibleDef, SqlSession sql, String purpose) throws EngineException
	{
		if (tpl != null)
		{
			if (!msgTplDB.exists(tpl, sql))
				throw new WrongArgumentException("Form has an unknown message template " + tpl);
			if (!compatibleDef.equals(msgTplDB.get(tpl, sql).getConsumer()))
				throw new WrongArgumentException("Template " + tpl + 
						" is not suitable as the " + purpose + " template");
		}
	}
	
	private void sendAttributeConfirmationRequest(RegistrationRequestState requestState,
			Long entityId, RegistrationForm form) throws InternalException, EngineException
	{
		for (Attribute<?> attr : requestState.getRequest().getAttributes())
		{
			if (attr == null)
				continue;
			
			if (attr.getAttributeSyntax().isVerifiable())
			{
				for (Object v : attr.getValues())
				{
					VerifiableElement val = (VerifiableElement) v;
					AttribiuteConfirmationState state = new AttribiuteConfirmationState(
							entityId, 
							attr.getName(), 
							val.getValue(), 
							requestState.getRequest().getUserLocale(), 
							attr.getGroupPath(), 
							getFormRedirectUrlForAttribute(requestState, form, attr));
					confirmationManager.sendConfirmationRequest(state);
				}
			}
		}
	}

	private void sendFormAttributeConfirmationRequest(RegistrationRequestState requestState, RegistrationForm form) 
			throws InternalException, EngineException
	{
		for (Attribute<?> attr : requestState.getRequest().getAttributes())
		{
			if (attr == null)
				continue;
			
			if (attr.getAttributeSyntax().isVerifiable())
			{
				for (Object v : attr.getValues())
				{
					VerifiableElement val = (VerifiableElement) v;
					if (val.isConfirmed())
						continue;
					RegistrationReqAttribiuteConfirmationState state = 
						new RegistrationReqAttribiuteConfirmationState(
							requestState.getRequestId(), 
							attr.getName(), 
							val.getValue(), 
							requestState.getRequest().getUserLocale(),
							attr.getGroupPath(), 
							getFormRedirectUrlForAttribute(requestState, form, attr));
					confirmationManager.sendConfirmationRequest(state);
				}
			}
		}
	}
	
	private void sendIdentityConfirmationRequest(RegistrationRequestState requestState,
			Long entityId, RegistrationForm form) throws InternalException, EngineException
	{
		for (IdentityParam id : requestState.getRequest().getIdentities())
		{
			if (id == null)
				continue;
			
			if (identityTypesRegistry.getByName(id.getTypeId()).isVerifiable() && !id.isConfirmed())
			{
				BaseConfirmationState state;
				if (entityId == null)
				{
					state = new RegistrationReqIdentityConfirmationState(
							requestState.getRequestId(),
							id.getTypeId(), id.getValue(), 
							requestState.getRequest().getUserLocale(),
							getFormRedirectUrlForIdentity(requestState, form, id));
				} else
				{
					state = new IdentityConfirmationState(entityId, 
							id.getTypeId(), id.getValue(), 
							requestState.getRequest().getUserLocale(),
							getFormRedirectUrlForIdentity(requestState, form, id));
				}
				confirmationManager.sendConfirmationRequest(state);
			}
		}
	}
	
	private String getFormRedirectUrlForIdentity(RegistrationRequestState requestState, RegistrationForm form,
			IdentityParam identity)
	{
		RegistrationTranslationProfile translationProfile = form.getTranslationProfile();
		return translationProfile.getPostConfirmationRedirectURL(form, requestState, identity, 
				requestState.getRequestId());
	}	
	
	private String getFormRedirectUrlForAttribute(RegistrationRequestState requestState, RegistrationForm form,
			Attribute<?> attr)
	{
		String current = null;
		if (InvocationContext.getCurrent().getCurrentURLUsed() != null
				&& InvocationContext.getCurrent().getLoginSession() == null)
			current = InvocationContext.getCurrent().getCurrentURLUsed();
		RegistrationTranslationProfile translationProfile = form.getTranslationProfile();
		String configured = translationProfile.getPostConfirmationRedirectURL(form, requestState, attr,
				requestState.getRequestId());
		return configured != null ? configured : current;
	}

	@Override
	@Transactional
	public String addInvitation(InvitationParam invitation) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		String randomUUID = UUID.randomUUID().toString();
		InvitationWithCode withCode = new InvitationWithCode(invitation, randomUUID);
		invitationDB.insert(randomUUID, withCode, SqlSessionTL.get());
		return randomUUID;
	}

	@Override
	@Transactional
	public void sendInvitation(String code) throws EngineException
	{
		/* TODO
		authz.checkAuthorization(AuthzCapability.maintenance);
		String userLocale = msg.getDefaultLocaleCode();
		InvitationWithCode invitation = invitationDB.get(code, SqlSessionTL.get());
		if (invitation.getContactAddress() == null || invitation.getFacilityId() == null)
			throw new WrongArgumentException("The invitation with the given code has no contact address configured");
		
		Map<String, String> notifyParams = new HashMap<>();
		notifyParams.put(BaseRegistrationTemplateDef.FORM_NAME, invitation.getFormId());
		notifyParams.put(key, );
		
		notificationProducer.sendNotification(invitation.getContactAddress(),
				invitation.getFacilityId(), templateId,
				notifyParams, userLocale);
				*/
	}

	@Override
	@Transactional
	public void removeInvitation(String code) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		invitationDB.remove(code, SqlSessionTL.get());
	}

	@Override
	@Transactional
	public List<InvitationWithCode> getInvitations() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return invitationDB.getAll(SqlSessionTL.get());
	}
}
