/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.enquiry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IdentityExistsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponsesChangedEvent;

@Component
class EnquiryResponseEditorController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryResponseEditorController.class);
	
	private final MessageSource msg;
	private final EnquiryManagement enquiryManagement;
	private final IdentityEditorRegistry identityEditorRegistry;
	private final CredentialEditorRegistry credentialEditorRegistry;
	private final AttributeHandlerRegistry attributeHandlerRegistry;
	private final AttributeTypeManagement atMan;	
	private final CredentialManagement credMan;
	private final GroupsManagement groupsMan;
	private final EntityManagement idMan;	
	private final AttributesManagement attrMan;
	private final IdPLoginController idpLoginController;
	private final PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder;
	private final PolicyAgreementManagement policyAgrMan;
	private final InvitationManagement invitationManagement;
	private final NotificationPresenter notificationPresenter;
	private final VaadinLogoImageLoader logoImageLoader;

	@Autowired
	public EnquiryResponseEditorController(MessageSource msg,
	                                       @Qualifier("insecure") EnquiryManagement enquiryManagement,
	                                       IdentityEditorRegistry identityEditorRegistry,
	                                       CredentialEditorRegistry credentialEditorRegistry,
	                                       AttributeHandlerRegistry attributeHandlerRegistry,
	                                       @Qualifier("insecure") AttributeTypeManagement atMan,
	                                       @Qualifier("insecure") CredentialManagement credMan,
	                                       @Qualifier("insecure") GroupsManagement groupsMan,
	                                       @Qualifier("insecure") EntityManagement idMan,
	                                       @Qualifier("insecure") AttributesManagement attrMan,
	                                       @Qualifier("insecure") InvitationManagement invitationMan,
	                                       IdPLoginController idpLoginController,
	                                       NotificationPresenter notificationPresenter,
	                                       PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder,
	                                       PolicyAgreementManagement policyAgrMan,
	                                       VaadinLogoImageLoader logoImageLoader)
	{
		this.msg = msg;
		this.enquiryManagement = enquiryManagement;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.atMan = atMan;
		this.credMan = credMan;
		this.groupsMan = groupsMan;
		this.idMan = idMan;
		this.attrMan = attrMan;
		this.idpLoginController = idpLoginController;
		this.notificationPresenter = notificationPresenter;
		this.policyAgreementsRepresentationBuilder = policyAgreementsRepresentationBuilder;
		this.policyAgrMan = policyAgrMan;
		this.invitationManagement = invitationMan;
		this.logoImageLoader = logoImageLoader;
	}

	private EnquiryResponseEditor getEditorInstance(EnquiryForm form, Map<String, Object> messageParams,
	                                                                                     RemotelyAuthenticatedPrincipal remoteContext, PrefilledSet set,
	                                                                                     List<PolicyAgreementConfiguration> filteredPolicyAgreement) throws Exception
	{
		return new EnquiryResponseEditor(msg, form, remoteContext,
				identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, atMan, credMan, groupsMan, notificationPresenter,
				policyAgreementsRepresentationBuilder, filteredPolicyAgreement, set, logoImageLoader, messageParams);
	}

	public EnquiryResponseEditor getEditorInstanceForUnauthenticatedUser(EnquiryForm form, Map<String, Object> messageParams,
	                                                                                                          RemotelyAuthenticatedPrincipal remoteContext, PrefilledSet prefilled,
	                                                                                                          EntityParam entityId) throws Exception
	{
		List<PolicyAgreementConfiguration> filteredPolicyAgreement = policyAgrMan.filterAgreementToPresent(
				entityId, form.getPolicyAgreements());
		return getEditorInstance(form, messageParams, remoteContext, prefilled, filteredPolicyAgreement);
	}

	public EnquiryResponseEditor getEditorInstanceForAuthenticatedUser(EnquiryForm form, PrefilledSet prefilled,
	                                                                   RemotelyAuthenticatedPrincipal remoteContext) throws Exception
	{
		EntityParam loggedEntity = getLoggedEntity();
		List<PolicyAgreementConfiguration> filteredPolicyAgreement = policyAgrMan.filterAgreementToPresent(
				loggedEntity,
				form.getPolicyAgreements());
		return getEditorInstance(form, Collections.emptyMap(), remoteContext, prefilled,
				filteredPolicyAgreement);
	}

	public EnquiryForm getForm(String name)
	{
		try
		{
			return enquiryManagement.getEnquiry(name);
		} catch (Exception e)
		{
			log.error("Can't load enquiry forms", e);
		}
		return null;
	}

	public boolean isStickyFormApplicableForCurrentUser(String formName)
	{
		EntityParam entity = getLoggedEntity();
		try
		{
			return enquiryManagement.getAvailableEnquires(entity, EnquirySelector.builder()
					.withAccessMode(EnquirySelector.AccessMode.NOT_BY_INVITATION_ONLY)
					.withType(EnquirySelector.Type.STICKY)
					.build()).stream().anyMatch(f -> f.getName().equals(formName));
		} catch (EngineException e)
		{
			log.error("Can't load sticky enquiry forms", e);
		}
		return false;
	}

	public List<EnquiryForm> getRegularFormsToFill()
	{
		EntityParam entity = getLoggedEntity();
		try
		{
			return enquiryManagement.getAvailableEnquires(entity, EnquirySelector.builder()
					.withAccessMode(EnquirySelector.AccessMode.NOT_BY_INVITATION_ONLY)
					.withType(EnquirySelector.Type.REGULAR)
					.build());
		} catch (EngineException e)
		{
			log.error("Can't load pending enquiry forms", e);
		}
		return Collections.emptyList();
	}

	public EnquiryResponseEditor getEditorInstanceForAuthenticatedUser(EnquiryForm form,
	                                                                   RemotelyAuthenticatedPrincipal remoteContext) throws Exception
	{
		EntityParam loggedEntity = getLoggedEntity();
		List<PolicyAgreementConfiguration> filteredPolicyAgreement = policyAgrMan.filterAgreementToPresent(
				loggedEntity,
				form.getPolicyAgreements());
		return getEditorInstance(form, Collections.emptyMap(), remoteContext, getPrefilledSetForSticky(form, loggedEntity),
				filteredPolicyAgreement);
	}

	private EntityParam getLoggedEntity()
	{
		return  new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId());
	}

	public boolean isFormApplicableForLoggedEntity(String formName, boolean ignoreByIvitationForms)
	{
		EntityParam entity = getLoggedEntity();
		try
		{
			return enquiryManagement.getAvailableEnquires(entity, EnquirySelector.builder()
					.withType(EnquirySelector.Type.ALL)
					.withAccessMode(ignoreByIvitationForms ? EnquirySelector.AccessMode.NOT_BY_INVITATION_ONLY : EnquirySelector.AccessMode.ANY)
					.build()).stream().anyMatch(f -> f.getName().equals(formName));
		} catch (EngineException e)
		{
			log.error("Can't load enquiry forms", e);
		}
		return false;
	}

	public PrefilledSet getPrefilledSetForSticky(EnquiryForm form, EntityParam entity) throws EngineException
	{		
		if (form.getType().equals(EnquiryType.STICKY))
		{	
			return new PrefilledSet(null, getPreffiledGroup(entity, form), getPrefilledAttribute(entity, form), null);
		
		}
			
		return new PrefilledSet();
	}

	private Map<Integer, PrefilledEntry<GroupSelection>> getPreffiledGroup(EntityParam entity, EnquiryForm form)
			throws EngineException
	{
		Map<String, GroupMembership> allGroups = idMan.getGroups(entity);

		Map<Integer, PrefilledEntry<GroupSelection>> prefilledGroupSelections = new HashMap<>();
		for (int i = 0; i < form.getGroupParams().size(); i++)
		{

			GroupRegistrationParam groupParam = form.getGroupParams().get(i);
			List<Group> allMatchingGroups = groupsMan.getGroupsByWildcard(groupParam.getGroupPath());
			List<Group> filterMatching = GroupPatternMatcher.filterByIncludeGroupsMode(GroupPatternMatcher.filterMatching(allMatchingGroups,
					allGroups.keySet().stream().sorted().collect(Collectors.toList())), groupParam.getIncludeGroupsMode());

			PrefilledEntry<GroupSelection> pe = new PrefilledEntry<>(new GroupSelection(
					filterMatching.stream().map(Group::getName).collect(Collectors.toList())),
					PrefilledEntryMode.DEFAULT);

			prefilledGroupSelections.put(i, pe);
		}
		return prefilledGroupSelections;
	}

	private Map<Integer, PrefilledEntry<Attribute>> getPrefilledAttribute(EntityParam entity, EnquiryForm form)
			throws EngineException
	{
		Map<Integer, PrefilledEntry<Attribute>> prefilledAttributes = new HashMap<>();

		Collection<AttributeExt> allAttributes =   attrMan.getAttributes(entity, null, null);
		
		for (int i = 0; i < form.getAttributeParams().size(); i++)
		{
			AttributeRegistrationParam attrParam = form.getAttributeParams().get(i);
			Collection<AttributeExt> attributes = allAttributes.stream()
					.filter(a -> a.getGroupPath().equals(attrParam.getGroup())
							&& a.getName().equals(attrParam.getAttributeType())).toList();

			prefilledAttributes.put(i, !attributes.isEmpty() ? (new PrefilledEntry<>(
					  attributes.iterator().next(),
					PrefilledEntryMode.DEFAULT)) : null);
	
		}

		return prefilledAttributes;
	}
	
	public void markFormAsIgnored(String formId)
	{
		EntityParam entity = new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId());
		try
		{
			enquiryManagement.ignoreEnquiry(formId, entity);
		} catch (EngineException e)
		{
			log.error("Can't mark form as ignored", e);
		}
	}

	public boolean checkIfRequestExistsForLoggedUser(String formName) throws EngineException
	{
		long entityId = getLoggedEntity().getEntityId();
		return checkIfRequestExists(formName, entityId);
	}

	private boolean checkIfRequestExists(String formName, long entity) throws EngineException
	{
		return enquiryManagement.getEnquiryResponses().stream().anyMatch(responseState ->
				responseState.getRequest().getFormId().equals(formName)
				&& responseState.getEntityId() == entity
				&& responseState.getStatus().equals(RegistrationRequestStatus.pending)
		);
	}

	public void removePendingRequest(String form) throws EngineException
	{
		enquiryManagement.removePendingStickyRequest(form, getLoggedEntity());
	}

	public WorkflowFinalizationConfiguration submitted(EnquiryResponse response, EnquiryForm form, 
			TriggeringMode mode, Optional<RewriteComboToEnquiryRequest> rewriteInvitationRequest) throws WrongArgumentException
	{
		RegistrationContext context = new RegistrationContext(
				idpLoginController.isLoginInProgress(), mode);
		try
		{
			if (rewriteInvitationRequest.isPresent())
			{
				rewriteComboToEnquiry(rewriteInvitationRequest.get());
			}
			String requestId = enquiryManagement.submitEnquiryResponse(response, context);
			WebSession.getCurrent().getEventBus().fireEvent(new EnquiryResponsesChangedEvent());
			return getFinalizationHandler(form).getFinalRegistrationConfigurationPostSubmit(requestId,
					getRequestStatus(requestId));
		} catch (IdentityExistsException e)
		{
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.PRESET_USER_EXISTS);
		} catch (WrongArgumentException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.warn("Registration request submision failed", e);
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.GENERAL_ERROR);
		}
	}
	
	private void rewriteComboToEnquiry(RewriteComboToEnquiryRequest request) throws EngineException
	{
		InvitationWithCode invitationWithCode = invitationManagement.getInvitation(request.invitationCode);
		if (invitationWithCode.getInvitation().getType().equals(InvitationType.COMBO))
		{
			FormPrefill prefill = invitationWithCode.getInvitation().getPrefillForForm(request.form);
			EnquiryInvitationParam param = new EnquiryInvitationParam(request.form.getName(),
					invitationWithCode.getInvitation().getExpiration(),
					invitationWithCode.getInvitation().getContactAddress());
			param.setFormPrefill(prefill);
			param.setEntity(request.entity);
			invitationManagement.updateInvitation(invitationWithCode.getRegistrationCode(), param);
		}
	}

	private RegistrationRequestStatus getRequestStatus(String requestId) 
	{
		try
		{
			return enquiryManagement.getEnquiryResponse(requestId).getStatus();
		} catch (Exception e)
		{
			log.error("Shouldn't happen: can't get request status, assuming rejected", e);
			return RegistrationRequestStatus.rejected;
		}
	}
	
	public WorkflowFinalizationConfiguration cancelled(EnquiryForm form, TriggeringMode mode,
			boolean markFormAsIgnored)
	{
		if (form.getType() == EnquiryType.REQUESTED_OPTIONAL)
		{
			if (markFormAsIgnored)
				markFormAsIgnored(form.getName());
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.IGNORED_ENQUIRY);
		} else
		{
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.CANCELLED);
		}
	}
	
	public PostFillingHandler getFinalizationHandler(EnquiryForm form)
	{
		String pageTitle = form.getPageTitle() == null ? null : form.getPageTitle().getValue(msg);
		return new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), false);
	}
}
