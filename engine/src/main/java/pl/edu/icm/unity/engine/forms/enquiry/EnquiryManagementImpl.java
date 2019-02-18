/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.EnquiryFilledTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.NewEnquiryTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipInfo;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.forms.BaseFormValidator;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport.Phase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;

/**
 * Implementation of the enquiry management API.
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class EnquiryManagementImpl implements EnquiryManagement
{
	private EnquiryFormDB enquiryFormDB;
	private EnquiryResponseDB requestDB;
	private NotificationProducer notificationProducer;
	private RegistrationConfirmationSupport confirmationsSupport;
	private UnityMessageSource msg;
	private InternalAuthorizationManager authz;
	private BaseFormValidator baseFormValidator;
	private EnquiryResponsePreprocessor enquiryResponseValidator;
	private SharedEndpointManagement sharedEndpointMan;
	private TransactionalRunner tx;
	private SharedEnquiryManagment internalManagment;
	private EntityResolver identitiesResolver;
	private AttributesHelper dbAttributes;
	private BulkGroupQueryService bulkService;
	
	@Autowired
	public EnquiryManagementImpl(EnquiryFormDB enquiryFormDB, EnquiryResponseDB requestDB,
			NotificationProducer notificationProducer,
			RegistrationConfirmationSupport confirmationsSupport,
			UnityMessageSource msg, InternalAuthorizationManager authz,
			BaseFormValidator baseFormValidator,
			EnquiryResponsePreprocessor enquiryResponseValidator,
			SharedEndpointManagement sharedEndpointMan, TransactionalRunner tx,
			SharedEnquiryManagment internalManagment, EntityResolver identitiesResolver,
			AttributesHelper dbAttributes,
			@Qualifier("insecure")
			BulkGroupQueryService bulkService)
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
		this.identitiesResolver = identitiesResolver;
		this.dbAttributes = dbAttributes;
		this.bulkService = bulkService;
	}

	@Transactional
	@Override
	public void addEnquiry(EnquiryForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateFormContents(form);
		enquiryFormDB.create(form);
	}
	
	@Transactional
	@Override
	public void sendEnquiry(String enquiryId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		EnquiryForm form = enquiryFormDB.get(enquiryId);
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		
		if (notificationsCfg.getEnquiryToFillTemplate() != null)
		{

			Map<String, String> params = new HashMap<>();
			params.put(NewEnquiryTemplateDef.FORM_NAME, form.getDisplayedName().getDefaultLocaleValue(msg));
			params.put(NewEnquiryTemplateDef.URL, 
					PublicRegistrationURLSupport.getWellknownEnquiryLink(enquiryId, sharedEndpointMan));
			
			
			GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/");
			Map<Long, GroupMembershipInfo> membershipInfo = bulkService.getMembershipInfo(bulkMembershipData);
			
			for (GroupMembershipInfo info : membershipInfo.values())
			{
				if (info.relevantEnquiryForm.contains(form.getName()))
				{
					notificationProducer.sendNotification(new EntityParam(info.entityInfo.getId()),
							notificationsCfg.getEnquiryToFillTemplate(), params,
							msg.getDefaultLocaleCode(), null, false);
				}
			}
		}
	}
	
	@Transactional
	@Override
	public void removeEnquiry(String formId, boolean dropRequests) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		internalManagment.removeForm(formId, dropRequests, requestDB, enquiryFormDB);
	}
	
	@Transactional
	@Override
	public void updateEnquiry(EnquiryForm updatedForm, boolean ignoreRequestsAndInvitations)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateFormContents(updatedForm);
		String formId = updatedForm.getName();
		if (!ignoreRequestsAndInvitations)
		{
			internalManagment.validateIfHasPendingRequests(formId, requestDB);
			internalManagment.validateIfHasInvitations(formId, InvitationType.ENQUIRY);
		}
		enquiryFormDB.update(updatedForm);
	}
	
	@Transactional
	@Override
	public List<EnquiryForm> getEnquires() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return enquiryFormDB.getAll();
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
		responseFull.setEntityId(getEntity(response.getFormId(), response.getRegistrationCode()));
		
		EnquiryForm form = recordRequestAndReturnForm(responseFull);
		sendNotificationOnNewResponse(form, response);
		boolean accepted = tryAutoProcess(form, responseFull, context);
		
		Long entityId = accepted ? responseFull.getEntityId() : null;
		tx.runInTransactionThrowing(() -> {
			confirmationsSupport.sendAttributeConfirmationRequest(responseFull, form, entityId,
					Phase.ON_SUBMIT);
			confirmationsSupport.sendIdentityConfirmationRequest(responseFull, form, entityId,
					Phase.ON_SUBMIT);
		});
		
		return responseFull.getRequestId();
	}

	private long getEntity(String formId, String code) throws EngineException
	{
		if (code == null)
		{
			return getLoggedEntity();
		} else
		{
			return getEntityFromInvitation(formId, code);
		}
	}

	private Long getLoggedEntity()
	{
		Long entityId = null;
		try
		{
			entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		} catch (Exception e)
		{
			throw new IllegalStateException("Can not get currently logged user");
		}
		return entityId;
	}
	
	private Long getEntityFromInvitation(String formId, String code) throws EngineException
	{

		return tx.runInTransactionRetThrowing(() -> {
			return enquiryResponseValidator.getEntityFromInvitationAndValidateCode(formId, code);
		});
	}

	@Override
	@Transactional
	public void processEnquiryResponse(String id, EnquiryResponse finalRequest,
			RegistrationRequestAction action, String publicCommentStr,
			String internalCommentStr) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.credentialModify, AuthzCapability.attributeModify,
				AuthzCapability.identityModify, AuthzCapability.groupModify);
		EnquiryResponseState currentRequest = requestDB.get(id);
		
		LoginSession client = internalManagment.preprocessRequest(finalRequest, currentRequest, action);
		
		AdminComment publicComment = internalManagment.preprocessComment(currentRequest, 
				publicCommentStr, client, true);
		AdminComment internalComment = internalManagment.preprocessComment(currentRequest, 
				internalCommentStr, client, false);

		EnquiryForm form = enquiryFormDB.get(currentRequest.getRequest().getFormId());

		switch (action)
		{
		case drop:
			internalManagment.dropEnquiryResponse(id);
			break;
		case reject:
			internalManagment.rejectEnquiryResponse(form, currentRequest, publicComment, internalComment);
			break;
		case update:
			updateResponse(form, currentRequest, publicComment, internalComment);
			break;
		case accept:
			internalManagment.acceptEnquiryResponse(form, currentRequest, publicComment, 
					internalComment, true);
			break;
		}
	}

	private void updateResponse(EnquiryForm form, EnquiryResponseState currentRequest,
			AdminComment publicComment, AdminComment internalComment) 
			throws EngineException
	{
		enquiryResponseValidator.validateSubmittedResponse(form, currentRequest.getRequest(), false);
		requestDB.update(currentRequest);
		internalManagment.sendProcessingNotification(form, 
				form.getNotificationsConfiguration().getUpdatedTemplate(),
				currentRequest, form.getName(), publicComment, internalComment);
	}
	
	private EnquiryForm recordRequestAndReturnForm(EnquiryResponseState responseFull) throws EngineException
	{
		return tx.runInTransactionRetThrowing(() -> {
			EnquiryForm form = enquiryFormDB.get(responseFull.getRequest().getFormId());
			enquiryResponseValidator.validateSubmittedResponse(form, responseFull.getRequest(), true);
			
			boolean isSticky = form.getType().equals(EnquiryType.STICKY);
			if (isSticky)
			{
				removeAllPendingRequestsOfForm(form.getName(), new EntityParam(responseFull.getEntityId()));
			}
			requestDB.create(responseFull);
			if (!isSticky)
			{
				addToAttribute(responseFull.getEntityId(),
						EnquiryAttributeTypesProvider.FILLED_ENQUIRES, form.getName());
			}
			return form;
		});
	}

	private void sendNotificationOnNewResponse(EnquiryForm form, EnquiryResponse response) throws EngineException
	{
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		if (notificationsCfg.getSubmittedTemplate() != null
				&& notificationsCfg.getAdminsNotificationGroup() != null)
		{
			Map<String, String> params = new HashMap<>();
			params.put(EnquiryFilledTemplateDef.FORM_NAME, form.getDisplayedName().getDefaultLocaleValue(msg));
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			params.put(EnquiryFilledTemplateDef.USER, loginSession.getEntityLabel());
			notificationProducer.sendNotificationToGroup(
					notificationsCfg.getAdminsNotificationGroup(), 
					notificationsCfg.getSubmittedTemplate(),
					params,
					msg.getDefaultLocaleCode());
		}
	}
	
	private boolean tryAutoProcess(EnquiryForm form, EnquiryResponseState requestFull, 
			RegistrationContext context) throws EngineException
	{
		return tx.runInTransactionRetThrowing(() -> {
			return internalManagment.autoProcessEnquiry(form, requestFull, 
						"Automatic processing of the request  " + 
						requestFull.getRequestId() + " invoked, action: {0}");
		});
	}

	
	private void validateFormContents(EnquiryForm form) throws EngineException
	{
		baseFormValidator.validateBaseFormContents(form);
		
		EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg == null)
			throw new WrongArgumentException("NotificationsConfiguration must be set in the form.");
		baseFormValidator.checkTemplate(notCfg.getSubmittedTemplate(), EnquiryFilledTemplateDef.NAME,
				"enquiry filled");
		baseFormValidator.checkTemplate(notCfg.getEnquiryToFillTemplate(), NewEnquiryTemplateDef.NAME,
				"new enquiry");
		baseFormValidator.checkTemplate(notCfg.getAcceptedTemplate(), AcceptRegistrationTemplateDef.NAME,
				"enquiry accepted");
		baseFormValidator.checkTemplate(notCfg.getRejectedTemplate(), RejectRegistrationTemplateDef.NAME,
				"enquiry rejected");
		
		if (form.getTargetGroups() == null || form.getTargetGroups().length == 0)
			throw new WrongArgumentException("Target groups must be set in the form.");
		if (form.getType() == null)
			throw new WrongArgumentException("Form type must be set.");
		if (form.getType().equals(EnquiryType.STICKY))
		{
			if (!form.getIdentityParams().isEmpty())
			{
				throw new WrongArgumentException("Identity params in sticky enquiry forms must be empty");
			}
			
			if (!form.getCredentialParams().isEmpty())
			{
				throw new WrongArgumentException("Credential params in sticky enquiry forms must be empty");
			}
		}	
	}
	
	@Transactional
	@Override
	public List<EnquiryResponseState> getEnquiryResponses() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.read);
		return requestDB.getAll();
	}

	@Transactional
	@Override
	public EnquiryResponseState getEnquiryResponse(String requestId)
	{
		authz.checkAuthorizationRT("/", AuthzCapability.read);
		return requestDB.get(requestId);
	}
	
	@Transactional
	@Override
	public List<EnquiryForm> getPendingEnquires(EntityParam entity) throws EngineException
	{
		long entityId = identitiesResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.readInfo);
		
		List<EnquiryForm> allForms = enquiryFormDB.getAll();
		
		Set<String> ignored = getEnquiresFromAttribute(entityId, 
				EnquiryAttributeTypesProvider.FILLED_ENQUIRES);
		ignored.addAll(getEnquiresFromAttribute(entityId, EnquiryAttributeTypesProvider.IGNORED_ENQUIRES));
	
		GroupMembershipInfo entityInfo = getMemebershipInfo(entityId);
	
		List<EnquiryForm> ret = new ArrayList<>();
		if (entityInfo == null)
			return ret;
		for (EnquiryForm form : allForms)
		{
			if (ignored.contains(form.getName()))
				continue;
			if (form.getType().equals(EnquiryType.STICKY))
				continue;
			if (form.isByInvitationOnly())
				continue;
			if (entityInfo.relevantEnquiryForm.contains(form.getName()))
				ret.add(form);
		}
		return ret;
	}
	
	@Transactional
	@Override
	public List<EnquiryForm> getAvailableStickyEnquires(EntityParam entity) throws EngineException
	{
		long entityId = identitiesResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.readInfo);
		List<EnquiryForm> allForms = enquiryFormDB.getAll();
		GroupMembershipInfo entityInfo = getMemebershipInfo(entityId);
		List<EnquiryForm> ret = new ArrayList<>();
		if (entityInfo == null)
			return ret;
		for (EnquiryForm form : allForms)
		{
			if (form.isByInvitationOnly())
				continue;
			
			if (form.getType().equals(EnquiryType.STICKY) &&
					entityInfo.relevantEnquiryForm.contains(form.getName()))
				ret.add(form);
		}
		return ret;
	}
		
	private GroupMembershipInfo getMemebershipInfo(Long entity) throws EngineException
	{
		GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/", Sets.newSet(entity));
		Map<Long, GroupMembershipInfo> membershipInfo = bulkService.getMembershipInfo(bulkMembershipData);	
		return membershipInfo.get(entity);	
	}
	
	private Set<String> getEnquiresFromAttribute(long entityId, String attributeName) 
			throws EngineException
	{
		Set<String> ret = new LinkedHashSet<>();
		Map<String, AttributeExt> attrs = dbAttributes.getAllAttributesAsMapOneGroup(entityId, "/");
		if (!attrs.containsKey(attributeName))
			return ret;
		
		AttributeExt attr = attrs.get(attributeName);
		attr.getValues().forEach(v -> ret.add(v.toString()));
		return ret;
	}
	
	private void addToAttribute(long entityId, String attributeName, String value) throws EngineException
	{
		Set<String> currentValues = getEnquiresFromAttribute(entityId, attributeName);
		currentValues.add(value);
		Attribute attribute = StringAttribute.of(attributeName, "/", new ArrayList<>(currentValues));
		dbAttributes.addAttribute(entityId, attribute, true, false);
	}
	
	private void removeAllPendingRequestsOfForm(String enquiryId, EntityParam entity)
	{
		for (EnquiryResponseState en : requestDB.getAll())
		{
			if (!en.getStatus().equals(RegistrationRequestStatus.pending))
				continue;
			EnquiryResponse res = en.getRequest();
			if (res.getFormId().equals(enquiryId))
			{
				requestDB.delete(en.getRequestId());
			}
		}
	}
	
	@Transactional
	@Override
	public void ignoreEnquiry(String enquiryId, EntityParam entity) throws EngineException
	{
		long entityId = identitiesResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
		EnquiryForm form = enquiryFormDB.get(enquiryId);
		if (form.getType().equals(EnquiryType.STICKY))
		{
			removeAllPendingRequestsOfForm(enquiryId, entity);
		} else
		{
			if (form.getType() == EnquiryType.REQUESTED_MANDATORY)
				throw new WrongArgumentException("The mandatory enquiry can not be marked as ignored");
			addToAttribute(entityId, EnquiryAttributeTypesProvider.IGNORED_ENQUIRES, enquiryId);
		}
	}

	@Transactional
	@Override
	public FormAutomationSupport getFormAutomationSupport(EnquiryForm form)
	{
		return confirmationsSupport.getEnquiryFormAutomationSupport(form);
	}

	@Transactional
	@Override
	public void removePendingStickyRequest(String form, EntityParam entity) throws EngineException
	{
		long entityId = identitiesResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
		EnquiryForm eform = enquiryFormDB.get(form);
		if (!eform.getType().equals(EnquiryType.STICKY))
			throw new WrongArgumentException("Only sticky enquiry request can be removed");
		removeAllPendingRequestsOfForm(form, entity);		
	}

	@Transactional
	@Override
	public EnquiryForm getEnquiry(String id) throws EngineException
	{
		return enquiryFormDB.get(id);
	}
}
