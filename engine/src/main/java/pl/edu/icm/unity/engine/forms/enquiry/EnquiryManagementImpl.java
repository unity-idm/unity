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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.EnquiryFilledTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.NewEnquiryTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.forms.BaseFormValidator;
import pl.edu.icm.unity.engine.forms.InvitationPrefillInfo;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport.Phase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.MembershipDAO;
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
	private AuthorizationManager authz;
	private BaseFormValidator baseFormValidator;
	private EnquiryResponseValidator enquiryResponseValidator;
	private SharedEndpointManagement sharedEndpointMan;
	private TransactionalRunner tx;
	private SharedEnquiryManagment internalManagment;
	private EntityResolver identitiesResolver;
	private AttributesHelper dbAttributes;
	private MembershipDAO dbShared;
	
	@Autowired
	public EnquiryManagementImpl(EnquiryFormDB enquiryFormDB, EnquiryResponseDB requestDB,
			NotificationProducer notificationProducer,
			RegistrationConfirmationSupport confirmationsSupport,
			UnityMessageSource msg, AuthorizationManager authz,
			BaseFormValidator baseFormValidator,
			EnquiryResponseValidator enquiryResponseValidator,
			SharedEndpointManagement sharedEndpointMan, TransactionalRunner tx,
			SharedEnquiryManagment internalManagment, EntityResolver identitiesResolver,
			AttributesHelper dbAttributes, MembershipDAO dbShared)
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
		this.dbShared = dbShared;
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
			
			for (String group: form.getTargetGroups())
				notificationProducer.sendNotificationToGroup(
					group, 
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
		internalManagment.removeForm(formId, dropRequests, requestDB, enquiryFormDB);
	}
	
	@Transactional
	@Override
	public void updateEnquiry(EnquiryForm updatedForm, boolean ignoreRequests) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateFormContents(updatedForm);
		String formId = updatedForm.getName();
		if (!ignoreRequests)
			internalManagment.validateIfHasPendingRequests(formId, requestDB);
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
		responseFull.setEntityId(InvocationContext.getCurrent().getLoginSession().getEntityId());
		
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
		enquiryResponseValidator.validateSubmittedRequest(form, currentRequest.getRequest(), 
				new InvitationPrefillInfo(), false);
		requestDB.update(currentRequest);
		internalManagment.sendProcessingNotification(form, 
				form.getNotificationsConfiguration().getUpdatedTemplate(),
				currentRequest, form.getName(), publicComment, internalComment);
	}
	
	private EnquiryForm recordRequestAndReturnForm(EnquiryResponseState responseFull) throws EngineException
	{
		return tx.runInTransactionRetThrowing(() -> {
			EnquiryForm form = enquiryFormDB.get(responseFull.getRequest().getFormId());
			enquiryResponseValidator.validateSubmittedRequest(form, responseFull.getRequest(), 
					new InvitationPrefillInfo(), true);
			requestDB.create(responseFull);
			addToAttribute(responseFull.getEntityId(), EnquiryAttributeTypesProvider.FILLED_ENQUIRES, 
					form.getName());
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
		if (!context.tryAutoAccept)
			return false;
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
	public List<EnquiryForm> getPendingEnquires(EntityParam entity) throws EngineException
	{
		long entityId = identitiesResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
		
		List<EnquiryForm> allForms = enquiryFormDB.getAll();
		
		Set<String> ignored = getEnquiresFromAttribute(entityId, 
				EnquiryAttributeTypesProvider.FILLED_ENQUIRES);
		ignored.addAll(getEnquiresFromAttribute(entityId, EnquiryAttributeTypesProvider.IGNORED_ENQUIRES));
		
		Set<String> allGroups = dbShared.getEntityMembershipSimple(entityId);
		
		List<EnquiryForm> ret = new ArrayList<>();
		for (EnquiryForm form: allForms)
		{
			if (ignored.contains(form.getName()))
				continue;
			if (isInTargetGroups(allGroups, form.getTargetGroups()))
				ret.add(form);
		}
		return ret;
	}

	private boolean isInTargetGroups(Set<String> groups, String[] targetGroups)
	{
		for (String targetGroup: targetGroups)
			if (groups.contains(targetGroup))
				return true;
		return false;
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
	
	@Transactional
	@Override
	public void ignoreEnquiry(String enquiryId, EntityParam entity) throws EngineException
	{
		long entityId = identitiesResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
		EnquiryForm form = enquiryFormDB.get(enquiryId);
		if (form.getType() == EnquiryType.REQUESTED_MANDATORY)
			throw new WrongArgumentException("The mandatory enquiry can not be marked as ignored");
		addToAttribute(entityId, EnquiryAttributeTypesProvider.IGNORED_ENQUIRES, enquiryId);
	}

	@Transactional
	@Override
	public FormAutomationSupport getFormAutomationSupport(EnquiryForm form)
	{
		return confirmationsSupport.getEnquiryFormAutomationSupport(form);
	}
}
