/**
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
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
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.ac.AttributeClassDB;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.db.generic.credreq.CredentialRequirementDB;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.internal.InternalRegistrationManagment;
import pl.edu.icm.unity.engine.notifications.NotificationProducerImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.registration.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.server.attributes.AttributeValueChecker;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
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

/**
 * Implementation of registrations subsystem.
 * 
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class RegistrationsManagementImpl implements RegistrationsManagement
{
	private DBSessionManager db;
	private RegistrationFormDB formsDB;
	private RegistrationRequestDB requestDB;
	private CredentialDB credentialDB;
	private CredentialRequirementDB credentialReqDB;
	private AttributeClassDB acDB;
	private DBAttributes dbAttributes;
	private MessageTemplateDB msgTplDB;
	
	private GroupResolver groupsResolver;
	private IdentityTypesRegistry identityTypesRegistry;
	private AuthorizationManager authz;
	private NotificationProducerImpl notificationProducer;
	private ConfirmationManager confirmationManager;
	private InternalRegistrationManagment internalManagment;
	private UnityMessageSource msg;

	@Autowired
	public RegistrationsManagementImpl(DBSessionManager db, RegistrationFormDB formsDB,
			RegistrationRequestDB requestDB, CredentialDB credentialDB,
			CredentialRequirementDB credentialReqDB, AttributeClassDB acDB,
			DBAttributes dbAttributes, GroupResolver groupsResolver, 
			IdentityTypesRegistry identityTypesRegistry, AuthorizationManager authz,
			NotificationProducerImpl notificationProducer, ConfirmationManager confirmationManager, 
			MessageTemplateDB msgTplDB, InternalRegistrationManagment internalManagment,
			UnityMessageSource msg)
	{
		this.db = db;
		this.formsDB = formsDB;
		this.requestDB = requestDB;
		this.credentialDB = credentialDB;
		this.credentialReqDB = credentialReqDB;
		this.acDB = acDB;
		this.dbAttributes = dbAttributes;
		this.groupsResolver = groupsResolver;
		this.identityTypesRegistry = identityTypesRegistry;
		this.authz = authz;
		this.notificationProducer = notificationProducer;
		this.confirmationManager = confirmationManager;
		this.msgTplDB = msgTplDB;
		this.internalManagment = internalManagment;
		this.msg = msg;
	}

	@Override
	public void addForm(RegistrationForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			validateFormContents(form, sql);
			formsDB.insert(form.getName(), form, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeForm(String formId, boolean dropRequests) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
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
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateForm(RegistrationForm updatedForm, boolean ignoreRequests)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
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
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public List<RegistrationForm> getForms() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sql = db.getSqlSession(false);
		try
		{
			return internalManagment.getForms(sql);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public String submitRegistrationRequest(RegistrationRequest request, boolean tryAutoAccept) throws EngineException
	{
		RegistrationRequestState requestFull = null;
		Long entityId = null;
		RegistrationForm form;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			form = formsDB.get(request.getFormId(), sql);
			internalManagment.validateRequestContents(form, request, true, true, sql);
			requestFull = new RegistrationRequestState();
			requestFull.setStatus(RegistrationRequestStatus.pending);
			requestFull.setRequest(request);
			requestFull.setRequestId(UUID.randomUUID().toString());
			requestFull.setTimestamp(new Date());
			requestDB.insert(requestFull.getRequestId(), requestFull, sql);
			sql.commit();
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
			if (tryAutoAccept && internalManagment.checkAutoAcceptCondition(requestFull.getRequest(), sql))
			{
				AdminComment autoAcceptComment = new AdminComment(
						InternalRegistrationManagment.AUTO_ACCEPT_COMMENT, 0, false);
				requestFull.getAdminComments().add(autoAcceptComment);
				entityId = internalManagment.acceptRequest(form, requestFull, null, 
						autoAcceptComment, false, sql);
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		if (entityId == null)
			sendFormAttributeConfirmationRequest(requestFull, form);
		else
			sendAttributeConfirmationRequest(requestFull, entityId, form);
		sendIdentityConfirmationRequest(requestFull, entityId, form);	
		
		
		return requestFull.getRequestId();
	}

	@Override
	public List<RegistrationRequestState> getRegistrationRequests() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.read);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<RegistrationRequestState> ret = requestDB.getAll(sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void processRegistrationRequest(String id, RegistrationRequest finalRequest,
			RegistrationRequestAction action, String publicCommentStr,
			String internalCommentStr) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.credentialModify, AuthzCapability.attributeModify,
				AuthzCapability.identityModify, AuthzCapability.groupModify);
		SqlSession sql = db.getSqlSession(true);
		try
		{
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
				dropRequest(id, sql);
				break;
			case reject:
				rejectRequest(form, currentRequest, publicComment, internalComment, sql);
				break;
			case update:
				updateRequest(form, currentRequest, publicComment, internalComment, sql);
				break;
			case accept:
				internalManagment.acceptRequest(form, currentRequest, publicComment, 
						internalComment, true, sql);
				break;
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	private void dropRequest(String id, SqlSession sql) throws EngineException
	{
		requestDB.remove(id, sql);
	}
	
	private void rejectRequest(RegistrationForm form, RegistrationRequestState currentRequest, 
			AdminComment publicComment, AdminComment internalComment, SqlSession sql) throws EngineException
	{
		currentRequest.setStatus(RegistrationRequestStatus.rejected);
		requestDB.update(currentRequest.getRequestId(), currentRequest, sql);
		RegistrationFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		internalManagment.sendProcessingNotification(notificationsCfg.getRejectedTemplate(), 
				currentRequest, currentRequest.getRequestId(), form.getName(),
				true, publicComment, 
				internalComment, notificationsCfg, sql);
	}
	
	private void updateRequest(RegistrationForm form, RegistrationRequestState currentRequest,
			AdminComment publicComment, AdminComment internalComment, SqlSession sql) 
			throws EngineException
	{
		internalManagment.validateRequestContents(form, currentRequest.getRequest(), false, true, sql);
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
		if (form.getAttributeAssignments() != null)
		{
			Set<String> used = new HashSet<>();
			for (Attribute<?> attr: form.getAttributeAssignments())
			{
				AttributeType at = atMap.get(attr.getName());
				if (at == null)
					throw new WrongArgumentException("Attribute type " + attr.getName() + 
							" does not exist");
				String key = at.getName() + " @ " + attr.getGroupPath();
				if (used.contains(key))
					throw new WrongArgumentException("Assigned attribute " + key + 
							" was specified more then once.");
				used.add(key);
				AttributeValueChecker.validate(attr, at);
				groupsResolver.resolveGroup(attr.getGroupPath(), gm);
			}
		}

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

		if (form.getAttributeClassAssignments() != null)
		{
			Set<String> acs = new HashSet<>();
			for (AttributeClassAssignment ac: form.getAttributeClassAssignments())
			{
				if (acs.contains(ac.getAcName()))
					throw new WrongArgumentException("Assigned attribute class " + 
							ac.getAcName() + " was specified more then once.");
				acs.add(ac.getAcName());
			}
			acDB.assertExist(acs, sql);
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

		if (form.getCredentialRequirementAssignment() == null)
			throw new WrongArgumentException("Credential requirement must be set for the form");
		if (credentialReqDB.get(form.getCredentialRequirementAssignment(), sql) == null)
			throw new WrongArgumentException("Credential requirement " + 
					form.getCredentialRequirementAssignment() + " does not exist");

		if (form.getGroupAssignments() != null)
		{
			Set<String> used = new HashSet<>();
			for (String group: form.getGroupAssignments())
			{
				groupsResolver.resolveGroup(group, gm);
				if (used.contains(group))
					throw new WrongArgumentException("Assigned group " + group + 
							" was specified more then once.");
				used.add(group);
			}
		}

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
		
		if (form.getInitialEntityState() == null)
			throw new WrongArgumentException("Initial entity state must be set in the form.");
		
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
		
		if (form.getRedirectAfterSubmit() != null)
		{
			try
			{
				new URI(form.getRedirectAfterSubmit());
			} catch (URISyntaxException e)
			{
				throw new WrongArgumentException("Redirect URL is invalid", e);
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
							attr.getGroupPath(), getFormRedirectUrl(form));
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
							attr.getGroupPath(), getFormRedirectUrl(form));
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
							getFormRedirectUrl(form));
				} else
				{
					state = new IdentityConfirmationState(entityId, 
							id.getTypeId(), id.getValue(), 
							requestState.getRequest().getUserLocale(),
							getFormRedirectUrl(form));
				}
				confirmationManager.sendConfirmationRequest(state);
			}
		}
	}
	
	private String getFormRedirectUrl(RegistrationForm form)
	{
		String url = null;
		if (InvocationContext.getCurrent().getCurrentURLUsed() != null
				&& InvocationContext.getCurrent().getLoginSession() == null)
			url = InvocationContext.getCurrent().getCurrentURLUsed();
		if (form.getRedirectAfterSubmit() != null
				&& !form.getRedirectAfterSubmit().equals(""))
			url = form.getRedirectAfterSubmit();
		return url;
	}
}
