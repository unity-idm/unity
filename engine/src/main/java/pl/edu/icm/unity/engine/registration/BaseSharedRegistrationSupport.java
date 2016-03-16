/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

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

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.registration.BaseRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.RegistrationWithCommentsTemplateDef;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.translation.form.GroupParam;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.utils.GroupUtils;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Implementation of the internal registration management. This is used
 * internally and not exposed by the public interfaces.
 * 
 * @author P. Piernik
 */
public class BaseSharedRegistrationSupport
{
	public static final String AUTO_PROCESS_COMMENT = "Automatically processed";

	protected UnityMessageSource msg;
	protected NotificationProducer notificationProducer;
	protected AttributesHelper attributesHelper;
	protected DBGroups dbGroups;
	protected EngineHelper engineHelper;

	public BaseSharedRegistrationSupport(UnityMessageSource msg,
			NotificationProducer notificationProducer,
			AttributesHelper attributesHelper, DBGroups dbGroups,
			EngineHelper engineHelper)
	{
		this.msg = msg;
		this.notificationProducer = notificationProducer;
		this.attributesHelper = attributesHelper;
		this.dbGroups = dbGroups;
		this.engineHelper = engineHelper;
	}

	protected void applyRequestedGroups(long entityId, Map<String, List<Attribute<?>>> remainingAttributesByGroup,
			TranslatedRegistrationRequest translatedRequest, SqlSession sql) throws EngineException
	{
		Map<String, GroupParam> sortedGroups = establishSortedGroups(translatedRequest.getGroups());

		EntityParam entity = new EntityParam(entityId);
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
			attributesHelper.addAttributesList(attributes, entityId,
					true, sql);
		}
	}

	private Map<String, GroupParam> establishSortedGroups(Collection<GroupParam> requestedGroups)
	{
		Map<String, GroupParam> sortedGroups = new TreeMap<>();
		Set<String> allGroups = new HashSet<>();
		allGroups.add("/");
		
		for (GroupParam group : requestedGroups)
		{
			Deque<String> missingGroups = GroupUtils.getMissingGroups(group.getGroup(), allGroups);
			for (String missingGroup: missingGroups)
			{
				sortedGroups.put(missingGroup, new GroupParam(missingGroup,
						group.getExternalIdp(), group.getTranslationProfile()));
				allGroups.add(missingGroup);
			}
		}
		return sortedGroups;
	}
	
	protected void applyRequestedAttributeClasses(TranslatedRegistrationRequest translatedRequest,
			long entityId, SqlSession sql) throws EngineException
	{
		Map<String, Set<String>> attributeClasses = translatedRequest.getAttributeClasses();
		for (Map.Entry<String, Set<String>> groupAcs: attributeClasses.entrySet())
		{
			attributesHelper.setAttributeClasses(entityId, groupAcs.getKey(), 
					groupAcs.getValue(), sql);
		}
	}
	
	protected void applyRequestedCredentials(UserRequestState<?> currentRequest, 
			long entityId, SqlSession sql) throws EngineException
	{
		BaseRegistrationInput originalRequest = currentRequest.getRequest();
		if (originalRequest.getCredentials() != null)
		{
			for (CredentialParamValue c : originalRequest.getCredentials())
			{
				engineHelper.setPreviouslyPreparedEntityCredentialInternal(
						entityId, c.getSecrets(),
						c.getCredentialId(), sql);
			}
		}
	}

	protected void addAttributeToGroupsMap(Attribute<?> a, List<Attribute<?>> rootAttributes,
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
		if (notificationsCfg.getChannel() == null || templateId == null || requesterAddress == null)
			return;
		Map<String, String> notifyParams = getBaseNotificationParams(formId, currentRequest.getRequestId());
		notifyParams.put(RegistrationWithCommentsTemplateDef.PUBLIC_COMMENT,
				publicComment == null ? "" : publicComment.getContents());
		notifyParams.put(RegistrationWithCommentsTemplateDef.INTERNAL_COMMENT, "");
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
	
	public <T extends UserRequestState<?>> void removeForm(String formId, 
			boolean dropRequests, GenericObjectsDB<T> requestDB, GenericObjectsDB<? extends BaseForm> formDB, 
			SqlSession sql) throws EngineException
	{
		List<T> requests = requestDB.getAll(sql);
		if (dropRequests)
		{
			for (T req: requests)
				if (formId.equals(req.getRequest().getFormId()))
					requestDB.remove(req.getRequestId(), sql);
		} else
		{
			for (T req: requests)
				if (formId.equals(req.getRequest().getFormId()))
					throw new SchemaConsistencyException("There are requests bound " +
							"to this form, and it was not chosen to drop them.");
		}

		formDB.remove(formId, sql);
	}
	
	public <T extends UserRequestState<?>> void validateIfHasPendingRequests(String formId, 
			GenericObjectsDB<T> requestDB, SqlSession sql) throws EngineException
	{
		List<T> requests = requestDB.getAll(sql);
		for (T req: requests)
			if (formId.equals(req.getRequest().getFormId()) && 
					req.getStatus() == RegistrationRequestStatus.pending)
				throw new SchemaConsistencyException("There are requests bound to " +
						"this form, and it was not chosen to ignore them.");
	}
}
