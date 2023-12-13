/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.capacity_limit.CapacityLimitName;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.EnquiryFilledTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.NewEnquiryTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.registration.*;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.capacityLimits.InternalCapacityLimitVerificator;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.forms.BaseFormValidator;
import pl.edu.icm.unity.engine.forms.InvitationPrefillInfo;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport;
import pl.edu.icm.unity.engine.forms.RegistrationConfirmationSupport.Phase;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

import java.util.*;
import java.util.stream.Collectors;

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
	private final EnquiryFormDB enquiryFormDB;
	private final EnquiryResponseDB requestDB;
	private final NotificationProducer notificationProducer;
	private final RegistrationConfirmationSupport confirmationsSupport;
	private final MessageSource msg;
	private final InternalAuthorizationManager authz;
	private final BaseFormValidator baseFormValidator;
	private final EnquiryResponsePreprocessor enquiryResponseValidator;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final TransactionalRunner tx;
	private final SharedEnquiryManagment internalManagment;
	private final EntityResolver identitiesResolver;
	private final AttributesHelper dbAttributes;
	private final BulkGroupQueryService bulkService;
	private final InternalCapacityLimitVerificator capacityLimitVerificator;
	private final EntityManagement entityManagement;

	@Autowired
	public EnquiryManagementImpl(EnquiryFormDB enquiryFormDB, EnquiryResponseDB requestDB,
			NotificationProducer notificationProducer, RegistrationConfirmationSupport confirmationsSupport,
			MessageSource msg, InternalAuthorizationManager authz, BaseFormValidator baseFormValidator,
			EnquiryResponsePreprocessor enquiryResponseValidator,
			PublicRegistrationURLSupport publicRegistrationURLSupport, TransactionalRunner tx,
			SharedEnquiryManagment internalManagment, EntityResolver identitiesResolver, AttributesHelper dbAttributes,
			@Qualifier("insecure") BulkGroupQueryService bulkService,
			InternalCapacityLimitVerificator capacityLimitVerificator,
			@Qualifier("insecure") EntityManagement entityManagement)
	{
		this.enquiryFormDB = enquiryFormDB;
		this.requestDB = requestDB;
		this.notificationProducer = notificationProducer;
		this.confirmationsSupport = confirmationsSupport;
		this.msg = msg;
		this.authz = authz;
		this.baseFormValidator = baseFormValidator;
		this.enquiryResponseValidator = enquiryResponseValidator;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
		this.tx = tx;
		this.internalManagment = internalManagment;
		this.identitiesResolver = identitiesResolver;
		this.dbAttributes = dbAttributes;
		this.bulkService = bulkService;
		this.capacityLimitVerificator = capacityLimitVerificator;
		this.entityManagement = entityManagement;
	}

	@Transactional
	@Override
	public void addEnquiry(EnquiryForm form) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		capacityLimitVerificator.assertInSystemLimitForSingleAdd(CapacityLimitName.EnquiryFormsCount,
				() -> enquiryFormDB.getCount());
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
			params.put(NewEnquiryTemplateDef.FORM_NAME, form.getDisplayedName()
					.getDefaultLocaleValue(msg));
			params.put(NewEnquiryTemplateDef.URL, publicRegistrationURLSupport.getWellknownEnquiryLink(enquiryId));

			GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/");
			Map<Long, EntityInGroupData> membershipInfo = bulkService.getMembershipInfo(bulkMembershipData);

			for (EntityInGroupData info : membershipInfo.values())
			{
				if (info.relevantEnquiryForms.contains(form.getName()))
				{
					notificationProducer.sendNotification(new EntityParam(info.entity.getId()),
							notificationsCfg.getEnquiryToFillTemplate(), params, msg.getDefaultLocaleCode(), null,
							false);
				}
			}
		}
	}

	@Transactional
	@Override
	public void removeEnquiry(String formId, boolean dropRequests) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		internalManagment.dropOrValidateFormRequests(formId, dropRequests);
		enquiryFormDB.delete(formId);
	}

	@Transactional
	@Override
	public void removeEnquiryWithoutDependencyChecking(String formId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		internalManagment.dropOrValidateFormRequests(formId, true);
		enquiryFormDB.deleteWithoutDependencyChecking(formId);
	}

	@Transactional
	@Override
	public void updateEnquiry(EnquiryForm updatedForm, boolean ignoreRequestsAndInvitations) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		validateFormContents(updatedForm);
		String formId = updatedForm.getName();
		if (!ignoreRequestsAndInvitations)
		{
			internalManagment.validateIfHasPendingRequests(formId);
			internalManagment.validateIfHasInvitations(updatedForm, InvitationType.ENQUIRY);
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
		responseFull.setRequestId(UUID.randomUUID()
				.toString());
		responseFull.setTimestamp(new Date());
		responseFull.setRegistrationContext(context);
		responseFull.setEntityId(getEntity(response.getFormId(), response.getRegistrationCode()));

		FormWithInvitation formWithInvitation = recordRequestAndReturnForm(responseFull);
		sendNotificationOnNewResponse(formWithInvitation.form);
		internalManagment.sendInvitationProcessedNotificationIfNeeded(formWithInvitation.form,
				formWithInvitation.invitation, responseFull);
		boolean accepted = tryAutoProcess(formWithInvitation.form, responseFull);

		Long entityId = accepted ? responseFull.getEntityId() : null;
		tx.runInTransactionThrowing(() ->
		{
			confirmationsSupport.sendAttributeConfirmationRequest(responseFull, formWithInvitation.form, entityId,
					Phase.ON_SUBMIT);
			confirmationsSupport.sendIdentityConfirmationRequest(responseFull, formWithInvitation.form, entityId,
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
			entityId = InvocationContext.getCurrent()
					.getLoginSession()
					.getEntityId();
		} catch (Exception e)
		{
			throw new IllegalStateException("Can not get currently logged user");
		}
		return entityId;
	}

	private Long getEntityFromInvitation(String formId, String code) throws EngineException
	{

		return tx.runInTransactionRetThrowing(() ->
		{
			return enquiryResponseValidator.getEntityFromInvitationAndValidateCode(formId, code);
		});
	}

	@Override
	@Transactional
	public void processEnquiryResponse(String id, EnquiryResponse finalRequest, RegistrationRequestAction action,
			String publicCommentStr, String internalCommentStr) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.credentialModify, AuthzCapability.attributeModify,
				AuthzCapability.identityModify, AuthzCapability.groupModify);
		EnquiryResponseState currentRequest = requestDB.get(id);

		LoginSession client = internalManagment.preprocessRequest(finalRequest, currentRequest, action);

		AdminComment publicComment = internalManagment.preprocessComment(currentRequest, publicCommentStr, client,
				true);
		AdminComment internalComment = internalManagment.preprocessComment(currentRequest, internalCommentStr, client,
				false);

		EnquiryForm form = enquiryFormDB.get(currentRequest.getRequest()
				.getFormId());

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
			internalManagment.acceptEnquiryResponse(form, currentRequest, publicComment, internalComment, true);
			break;
		}
	}

	private void updateResponse(EnquiryForm form, EnquiryResponseState currentRequest, AdminComment publicComment,
			AdminComment internalComment) throws EngineException
	{
		enquiryResponseValidator.validateSubmittedResponse(form, currentRequest, false);
		requestDB.update(currentRequest);
		internalManagment.sendProcessingNotification(form, form.getNotificationsConfiguration()
				.getUpdatedTemplate(), currentRequest, form.getName(), publicComment, internalComment);
	}

	private FormWithInvitation recordRequestAndReturnForm(EnquiryResponseState responseFull) throws EngineException
	{
		return tx.runInTransactionRetThrowing(() ->
		{
			EnquiryForm form = enquiryFormDB.get(responseFull.getRequest()
					.getFormId());
			InvitationPrefillInfo validateSubmittedResponse = enquiryResponseValidator.validateSubmittedResponse(form,
					responseFull, true);

			boolean isSticky = form.getType()
					.equals(EnquiryType.STICKY);
			if (isSticky)
			{
				removeAllPendingRequestsOfForm(form.getName(), new EntityParam(responseFull.getEntityId()));
			}
			requestDB.create(responseFull);
			if (!isSticky)
			{
				addToAttribute(responseFull.getEntityId(), EnquiryAttributeTypesProvider.FILLED_ENQUIRES,
						form.getName());
			}
			return new FormWithInvitation(form, validateSubmittedResponse);
		});
	}

	private void sendNotificationOnNewResponse(EnquiryForm form) throws EngineException
	{
		EnquiryFormNotifications notificationsCfg = form.getNotificationsConfiguration();
		if (notificationsCfg.getSubmittedTemplate() != null && notificationsCfg.getAdminsNotificationGroup() != null)
		{
			Map<String, String> params = new HashMap<>();
			params.put(EnquiryFilledTemplateDef.FORM_NAME, form.getDisplayedName()
					.getDefaultLocaleValue(msg));
			LoginSession loginSession = InvocationContext.getCurrent()
					.getLoginSession();
			params.put(EnquiryFilledTemplateDef.USER, loginSession.getEntityLabel());
			notificationProducer.sendNotificationToGroup(notificationsCfg.getAdminsNotificationGroup(),
					notificationsCfg.getSubmittedTemplate(), params, msg.getDefaultLocaleCode());
		}
	}

	private boolean tryAutoProcess(EnquiryForm form, EnquiryResponseState requestFull) throws EngineException
	{
		return tx.runInTransactionRetThrowing(() ->
		{
			return internalManagment.autoProcessEnquiry(form, requestFull,
					"Automatic processing of the request  " + requestFull.getRequestId() + " invoked, action: {0}");
		});
	}

	private void validateFormContents(EnquiryForm form) throws EngineException
	{
		baseFormValidator.validateBaseFormContents(form);

		EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg == null)
			throw new WrongArgumentException("NotificationsConfiguration must be set in the form.");
		baseFormValidator.checkTemplate(notCfg.getSubmittedTemplate(), EnquiryFilledTemplateDef.NAME, "enquiry filled");
		baseFormValidator.checkTemplate(notCfg.getEnquiryToFillTemplate(), NewEnquiryTemplateDef.NAME, "new enquiry");
		baseFormValidator.checkTemplate(notCfg.getAcceptedTemplate(), AcceptRegistrationTemplateDef.NAME,
				"enquiry accepted");
		baseFormValidator.checkTemplate(notCfg.getRejectedTemplate(), RejectRegistrationTemplateDef.NAME,
				"enquiry rejected");

		if (form.getTargetGroups() == null || form.getTargetGroups().length == 0)
			throw new WrongArgumentException("Target groups must be set in the form.");
		for (String targetGroup : form.getTargetGroups())
		{
			if (!GroupPatternMatcher.isValidPattern(targetGroup))
				throw new IllegalArgumentException(targetGroup +  
						" is not a valid target group: must start with '/'");
		}
		
		if (form.getType() == null)
			throw new WrongArgumentException("Form type must be set.");
		if (form.getType()
				.equals(EnquiryType.STICKY))
		{
			if (!form.getIdentityParams()
					.isEmpty())
			{
				throw new WrongArgumentException("Identity params in sticky enquiry forms must be empty");
			}

			if (!form.getCredentialParams()
					.isEmpty())
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
	public List<EnquiryForm> getAvailableEnquires(EntityParam entityParam, EnquirySelector selector)
			throws EngineException
	{
		Entity entity = entityManagement.getEntity(entityParam);
		authz.checkAuthorization(authz.isSelf(entity.getId()), AuthzCapability.readInfo);
		List<EnquiryForm> filteredEnquiryForms = enquiryFormDB.getAll()
				.stream()
				.filter(selector.type.filter)
				.filter(selector.accessMode.filter)
				.collect(Collectors.toList());

		if (filteredEnquiryForms.isEmpty())
			return Collections.emptyList();

		List<EnquiryForm> relevantEnquiryForms = getApplicableEnquiries(entity, filteredEnquiryForms);
		
		if (selector.type.equals(EnquirySelector.Type.STICKY))
		{
			return relevantEnquiryForms;
		}
		
		return filterIgnoredOrFilledFromRegularEnquires(entity, relevantEnquiryForms);		
		
	}
	
	private List<EnquiryForm> filterIgnoredOrFilledFromRegularEnquires(Entity entity,
			List<EnquiryForm> userApplicableEnquiries) throws EngineException
	{
		Set<String> ignoredOrFilledEqnuiries = getEnquiresFromAttribute(entity.getId(),
				EnquiryAttributeTypesProvider.FILLED_ENQUIRES);
		ignoredOrFilledEqnuiries
				.addAll(getEnquiresFromAttribute(entity.getId(), EnquiryAttributeTypesProvider.IGNORED_ENQUIRES));

		return userApplicableEnquiries.stream()
				.filter(f -> f.getType()
						.equals(EnquiryType.STICKY) || !ignoredOrFilledEqnuiries.contains(f.getName()))
				.collect(Collectors.toList());
	}

	private List<EnquiryForm> getApplicableEnquiries(Entity entity, List<EnquiryForm> allForms) throws EngineException
	{
		List<EnquiryForm> forms = new ArrayList<>();
		Set<String> entityGroups = entityManagement.getGroups(new EntityParam(entity.getId()))
				.keySet();
		Collection<AttributeExt> entityAttributes = dbAttributes.getAllAttributesAsMapOneGroup(entity.getId(), "/")
				.values();
		for (EnquiryForm enqForm : allForms)
		{
			if (EnquiryTargetCondEvaluator.evaluateTargetCondition(enqForm, entity.getIdentities(), entity.getState()
					.toString(), entity.getCredentialInfo(), entityGroups, entityAttributes))
				forms.add(enqForm);
		}
		return forms;
	}

	private Set<String> getEnquiresFromAttribute(long entityId, String attributeName) throws EngineException
	{
		Set<String> ret = new LinkedHashSet<>();
		Map<String, AttributeExt> attrs = dbAttributes.getAllAttributesAsMapOneGroup(entityId, "/");
		if (!attrs.containsKey(attributeName))
			return ret;

		AttributeExt attr = attrs.get(attributeName);
		attr.getValues()
				.forEach(v -> ret.add(v.toString()));
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
			throws IllegalIdentityValueException
	{
		for (EnquiryResponseState en : requestDB.getAll())
		{
			if (!en.getStatus()
					.equals(RegistrationRequestStatus.pending))
				continue;
			EnquiryResponse res = en.getRequest();
			long entityId = identitiesResolver.getEntityId(entity);

			if (res.getFormId()
					.equals(enquiryId) && en.getEntityId() == entityId)
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
		if (form.getType()
				.equals(EnquiryType.STICKY))
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
		if (!eform.getType()
				.equals(EnquiryType.STICKY))
			throw new WrongArgumentException("Only sticky enquiry request can be removed");
		removeAllPendingRequestsOfForm(form, entity);
	}

	@Transactional
	@Override
	public EnquiryForm getEnquiry(String id) throws EngineException
	{
		return enquiryFormDB.get(id);
	}

	@Override
	@Transactional
	public boolean hasForm(String id)
	{
		return enquiryFormDB.exists(id);
	}

	private static class FormWithInvitation
	{
		public final EnquiryForm form;
		public final InvitationPrefillInfo invitation;

		public FormWithInvitation(EnquiryForm form, InvitationPrefillInfo invitation)
		{
			this.form = form;
			this.invitation = invitation;
		}
	}
}
