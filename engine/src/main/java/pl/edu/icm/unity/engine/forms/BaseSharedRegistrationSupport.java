/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.reg.BaseRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationProcessedNotificationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RegistrationWithCommentsTemplateDef;
import pl.edu.icm.unity.base.registration.AdminComment;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.BaseFormNotifications;
import pl.edu.icm.unity.base.registration.BaseRegistrationInput;
import pl.edu.icm.unity.base.registration.CredentialParamValue;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.engine.identity.SecondFactorOptInService;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;

/**
 * Implementation of the internal registration management. This is used
 * internally and not exposed by the public interfaces.
 * 
 * @author P. Piernik
 */
public class BaseSharedRegistrationSupport
{	
	private static final Logger log = Log.getLogger(Log.U_SERVER_FORMS,
			BaseSharedRegistrationSupport.class);
	
	public static final String AUTO_PROCESS_COMMENT = "Automatically processed";
	public static final String AUTO_PROCESS_INVITATIONS_COMMENT = "Automatically processed invitations";
	protected final EntityResolver entityResolver;
	protected final MessageSource msg;
	protected final NotificationProducer notificationProducer;
	protected final AttributesHelper attributesHelper;
	protected final GroupHelper groupHelper;
	protected final EntityCredentialsHelper credentialHelper;
	protected final InternalFacilitiesManagement facilitiesManagement;
	private final InvitationDB invitationDB;
	protected final PolicyAgreementManagement policyAgreementManagement;
	private final SecondFactorOptInService secondFactorOptInService;
	private final NamedCRUDDAOWithTS<? extends UserRequestState<?>> requestDB;

	public BaseSharedRegistrationSupport(MessageSource msg,
			NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, GroupHelper groupHelper,
			EntityCredentialsHelper entityCredentialsHelper,
			InternalFacilitiesManagement facilitiesManagement,
			InvitationDB invitationDB, PolicyAgreementManagement policyAgreementManagement,
			SecondFactorOptInService secondFactorOptInService,
			NamedCRUDDAOWithTS<? extends UserRequestState<?>> requestDB,
			EntityResolver entityResolver)
	{
		this.msg = msg;
		this.notificationProducer = notificationProducer;
		this.attributesHelper = attributesHelper;
		this.groupHelper = groupHelper;
		this.credentialHelper = entityCredentialsHelper;
		this.facilitiesManagement = facilitiesManagement;
		this.invitationDB =  invitationDB;
		this.policyAgreementManagement = policyAgreementManagement;
		this.secondFactorOptInService = secondFactorOptInService;
		this.requestDB = requestDB;
		this.entityResolver = entityResolver;
	}

	protected void applyRequestedGroups(long entityId, Map<String, List<Attribute>> remainingAttributesByGroup,
			Collection<GroupParam> requestedGroups, List<Group> actualGroups) throws EngineException
	{
		Map<String, GroupParam> sortedGroups = establishSortedGroups(requestedGroups, actualGroups);
		
		EntityParam entity = new EntityParam(entityId);
		for (Map.Entry<String, GroupParam> entry : sortedGroups.entrySet())
		{
			List<Attribute> attributes = remainingAttributesByGroup.get(entry.getKey());
			if (attributes == null)
				attributes = Collections.emptyList();
			attributesHelper.checkGroupAttributeClassesConsistency(attributes, entry.getKey());
			GroupParam sel = entry.getValue();
			String idp = sel == null ? null : sel.getExternalIdp();
			String profile = sel == null ? null : sel.getTranslationProfile();
			groupHelper.addMemberFromParent(entry.getKey(), entity, idp, profile, new Date());
			attributesHelper.addAttributesList(attributes, entityId, true);
		}
	}

	private Map<String, GroupParam> establishSortedGroups(Collection<GroupParam> requestedGroups, List<Group> actualGroups)
	{
		Map<String, GroupParam> sortedGroups = new TreeMap<>();
		Set<String> allGroups = new HashSet<>();
		if (actualGroups != null && !actualGroups.isEmpty())
		{
			allGroups.addAll(actualGroups.stream().map(g -> g.toString()).collect(Collectors.toList()));
		} else
		{
			allGroups.add("/");
		}
		for (GroupParam group : requestedGroups)
		{
			Deque<String> missingGroups = Group.getMissingGroups(group.getGroup(), allGroups);
			for (String missingGroup: missingGroups)
			{
				sortedGroups.put(missingGroup, new GroupParam(missingGroup,
						group.getExternalIdp(), group.getTranslationProfile()));
				allGroups.add(missingGroup);
			}
		}
		return sortedGroups;
	}
	
	protected void applyRequestedAttributeClasses(Map<String, Set<String>> attributeClasses,
			long entityId) throws EngineException
	{
		for (Map.Entry<String, Set<String>> groupAcs: attributeClasses.entrySet())
		{
			if (groupHelper.isMember(entityId, groupAcs.getKey()))
			{
				attributesHelper.setAttributeClasses(entityId, groupAcs.getKey(), groupAcs.getValue());
			}
		}
	}
	
	protected void applyRequestedCredentials(UserRequestState<?> currentRequest, 
			long entityId) throws EngineException
	{
		BaseRegistrationInput originalRequest = currentRequest.getRequest();
		if (originalRequest.getCredentials() != null)
		{
			for (CredentialParamValue c : originalRequest.getCredentials())
			{
				credentialHelper.setPreviouslyPreparedEntityCredential(
						entityId, c.getSecrets(),
						c.getCredentialId());
			}
		}
	}

	protected void applyMFAStatus(long entityId, Boolean mfaPreferenceStatus) throws EngineException
	{
		if (mfaPreferenceStatus != null)
			secondFactorOptInService.setUserMFAOptIn(entityId, mfaPreferenceStatus);
	}
	
	protected void addAttributeToGroupsMap(Attribute a, List<Attribute> rootAttributes,
			Map<String, List<Attribute>> remainingAttributesByGroup)
	{
		String path = a.getGroupPath();
		if (path.equals("/"))
			rootAttributes.add(a);
		else
		{
			List<Attribute> attrs = remainingAttributesByGroup.get(path);
			if (attrs == null)
			{
				attrs = new ArrayList<>();
				remainingAttributesByGroup.put(path, attrs);
			}
			attrs.add(a);
		}
	}

	
	public AdminComment preprocessComment(UserRequestState<?> currentRequest,
			String publicCommentStr, LoginSession client, boolean publicComment)
	{
		AdminComment comment = null;
		if (publicCommentStr != null)
		{
			comment = new AdminComment(publicCommentStr, client.getEntityId(), publicComment);
			currentRequest.getAdminComments().add(comment);
		}
		return comment;
	}
	
	public <T extends BaseRegistrationInput> LoginSession preprocessRequest(
			T finalRequest, UserRequestState<T> currentRequest,
			RegistrationRequestAction action) throws WrongArgumentException
	{
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

		if (currentRequest.getStatus() != RegistrationRequestStatus.pending && 
				(action == RegistrationRequestAction.accept || 
				action == RegistrationRequestAction.reject))
			throw new WrongArgumentException("The request was already processed. " +
					"It is only possible to drop it or to modify its comments.");
		if (currentRequest.getStatus() != RegistrationRequestStatus.pending && 
				action == RegistrationRequestAction.update && finalRequest != null)
			throw new WrongArgumentException("The request was already processed. " +
					"It is only possible to drop it or to modify its comments.");
		return client;
	}
	
	public Map<String, String> getBaseNotificationParams(String formId, String requestId)
	{
		Map<String, String> ret = new HashMap<>();
		ret.put(BaseRegistrationTemplateDef.FORM_NAME, formId);
		ret.put(BaseRegistrationTemplateDef.REQUEST_ID, requestId);
		return ret;
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
			UserRequestState<?> currentRequest, String formId,
			boolean sendToRequester, AdminComment publicComment,
			AdminComment internalComment,
			BaseFormNotifications notificationsCfg, String requesterAddress)
			throws EngineException
	{
		if (templateId == null || requesterAddress == null)
			return;
		Map<String, String> notifyParams = getBaseNotificationParams(formId, currentRequest.getRequestId());
		notifyParams.put(RegistrationWithCommentsTemplateDef.PUBLIC_COMMENT,
				publicComment == null ? "" : publicComment.getContents());
		notifyParams.put(RegistrationWithCommentsTemplateDef.INTERNAL_COMMENT, "");

		if (sendToRequester || publicComment != null)
		{
			String userLocale = currentRequest.getRequest().getUserLocale();
			notificationProducer.sendNotification(requesterAddress, templateId,
					notifyParams, userLocale);
		}

		boolean sendToAdmin = sendToRequester == false || 
				notificationsCfg.isSendUserNotificationCopyToAdmin();
		if (notificationsCfg.getAdminsNotificationGroup() != null && sendToAdmin)
		{
			notifyParams.put(
					RegistrationWithCommentsTemplateDef.INTERNAL_COMMENT,
					internalComment == null ? "" : internalComment
							.getContents());
			notificationProducer.sendNotificationToGroup(
					notificationsCfg.getAdminsNotificationGroup(), templateId,
					notifyParams, msg.getDefaultLocaleCode());
		}
	}
	
	public void dropOrValidateFormRequests(String formId, 
			boolean dropRequests) throws EngineException
	{
		List<? extends UserRequestState<?>> requests = requestDB.getAll();
		if (dropRequests)
		{
			dropFormRequests(formId, requests);
		} else
		{
			assertThereAreNoRequests(formId, requests);
		}
	}
	
	private void dropFormRequests(String formId, List<? extends UserRequestState<?>> requests)
	{
		for (UserRequestState<?> req: requests)
			if (formId.equals(req.getRequest().getFormId()))
				requestDB.delete(req.getRequestId());
		
	}
	
	private void assertThereAreNoRequests(String formId, List<? extends UserRequestState<?>> requests) throws SchemaConsistencyException
	{
		for (UserRequestState<?> req: requests)
			if (formId.equals(req.getRequest().getFormId()))
				throw new SchemaConsistencyException("There are requests bound " +
						"to this form, and it was not chosen to drop them.");	
	}
	
	public void validateIfHasPendingRequests(String formId) throws EngineException
	{
		List<? extends UserRequestState<?>> requests = requestDB.getAll();
		for (UserRequestState<?> req: requests)
			if (formId.equals(req.getRequest().getFormId()) && 
					req.getStatus() == RegistrationRequestStatus.pending)
				throw new SchemaConsistencyException("There are requests bound to " +
						"this form, and it was not chosen to ignore them.");
	}
	
	public void validateIfHasInvitations(BaseForm formId, InvitationType type) throws EngineException
	{
		if (invitationDB.getAll().stream().filter(i -> {
			try
			{
				return i.getInvitation().getType().equals(type)
						&& i.getInvitation().matchesForm(formId);
			} catch (IllegalFormTypeException e)
			{
				log.error("Invalid form type", e);
				return false;
			}
		}).count() > 0)
			throw new SchemaConsistencyException("There are invitations created for "
					+ "this form, and it was not chosen to ignore them.");
	}
	
	public void sendInvitationProcessedNotificationIfNeeded(BaseForm form, InvitationPrefillInfo invitationInfo,
			UserRequestState<?> requestFull) throws EngineException
	{	
		if (!invitationInfo.isByInvitation()
				|| form.getNotificationsConfiguration().getInvitationProcessedTemplate() == null
				|| invitationInfo.getInvitation().get().getInvitation().getInviterEntity().isEmpty())
		{
			return;
		}
		
		Long inviterEntity = invitationInfo.getInvitation().get().getInvitation().getInviterEntity().get();
		try
		{
			entityResolver.getEntityId(new EntityParam(inviterEntity));
		} catch (UnknownIdentityException e)
		{
			log.debug("Inviter entity does not exists, skipping sending invitation processed message", e);
			return;
		}
	
		InvitationWithCode invitationWithCode = invitationInfo.getInvitation().get();
		Map<String, String> params = getBaseNotificationParams(form.getName(), requestFull.getRequestId());
		ZonedDateTime createTime = invitationWithCode.getCreationTime().atZone(ZoneId.systemDefault());
		params.put(InvitationProcessedNotificationTemplateDef.CREATION_TIME,
				createTime.format(DateTimeFormatter.RFC_1123_DATE_TIME));
		params.put(InvitationProcessedNotificationTemplateDef.CONTACT_ADDRESS,
				invitationWithCode.getInvitation().getContactAddress());
		try
		{
			notificationProducer.sendNotification(
					new EntityParam(inviterEntity),
					form.getNotificationsConfiguration().getInvitationProcessedTemplate(), params,
					msg.getDefaultLocaleCode(), null, false);
		} catch (IllegalIdentityValueException e)
		{
			log.trace("Can not get address for entity " + invitationWithCode.getInvitation().getInviterEntity().get(),
					e);
		}
	}
}
