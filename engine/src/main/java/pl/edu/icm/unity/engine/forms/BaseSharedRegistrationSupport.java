/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

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

import pl.edu.icm.unity.base.msgtemplates.reg.BaseRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RegistrationWithCommentsTemplateDef;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.group.GroupHelper;
import pl.edu.icm.unity.engine.notifications.InternalFacilitiesManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;

/**
 * Implementation of the internal registration management. This is used
 * internally and not exposed by the public interfaces.
 * 
 * @author P. Piernik
 */
public class BaseSharedRegistrationSupport
{	
	public static final String AUTO_PROCESS_COMMENT = "Automatically processed";
	public static final String AUTO_PROCESS_INVITATIONS_COMMENT = "Automatically processed invitations";

	protected UnityMessageSource msg;
	protected NotificationProducer notificationProducer;
	protected AttributesHelper attributesHelper;
	protected GroupHelper groupHelper;
	protected EntityCredentialsHelper credentialHelper;
	protected InternalFacilitiesManagement facilitiesManagement;
	private InvitationDB invitationDB;

	public BaseSharedRegistrationSupport(UnityMessageSource msg,
			NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, GroupHelper groupHelper,
			EntityCredentialsHelper entityCredentialsHelper,
			InternalFacilitiesManagement facilitiesManagement,
			InvitationDB invitationDB)
	{
		this.msg = msg;
		this.notificationProducer = notificationProducer;
		this.attributesHelper = attributesHelper;
		this.groupHelper = groupHelper;
		this.credentialHelper = entityCredentialsHelper;
		this.facilitiesManagement = facilitiesManagement;
		this.invitationDB =  invitationDB;
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
	
	public <T extends UserRequestState<?>> void removeForm(String formId, 
			boolean dropRequests, NamedCRUDDAOWithTS<T> requestDB, 
			NamedCRUDDAOWithTS<? extends BaseForm> formDB) throws EngineException
	{
		List<T> requests = requestDB.getAll();
		if (dropRequests)
		{
			for (T req: requests)
				if (formId.equals(req.getRequest().getFormId()))
					requestDB.delete(req.getRequestId());
		} else
		{
			for (T req: requests)
				if (formId.equals(req.getRequest().getFormId()))
					throw new SchemaConsistencyException("There are requests bound " +
							"to this form, and it was not chosen to drop them.");
		}

		formDB.delete(formId);
	}
	
	public <T extends UserRequestState<?>> void validateIfHasPendingRequests(String formId, 
			NamedCRUDDAOWithTS<T> requestDB) throws EngineException
	{
		List<T> requests = requestDB.getAll();
		for (T req: requests)
			if (formId.equals(req.getRequest().getFormId()) && 
					req.getStatus() == RegistrationRequestStatus.pending)
				throw new SchemaConsistencyException("There are requests bound to " +
						"this form, and it was not chosen to ignore them.");
	}
	
	public void validateIfHasInvitations(String formId, InvitationType type) throws EngineException
	{
		if (invitationDB.getAll().stream().filter(i -> i.getInvitation().getType().equals(type)
				&& i.getInvitation().getFormId().equals(formId)).count() > 0)
			throw new SchemaConsistencyException("There are invitations created for "
					+ "this form, and it was not chosen to ignore them.");
	}
}
