/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.forms.enquiry;

import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.WebSession;
import io.imunity.vaadin23.shared.endpoint.forms.policy_agreements.PolicyAgreementRepresentationBuilderV23;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.AttributeHandlerRegistryV23;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorRegistryV23;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditorRegistryV23;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.*;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IdentityExistsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.*;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.registration.*;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.invite.*;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseChangedEvent;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class EnquiryResponseEditorControllerV23
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryResponseEditorControllerV23.class);
	
	private final MessageSource msg;
	private final EnquiryManagement enquiryManagement;
	private final IdentityEditorRegistryV23 identityEditorRegistry;
	private final CredentialEditorRegistryV23 credentialEditorRegistry;
	private final AttributeHandlerRegistryV23 attributeHandlerRegistry;
	private final AttributeTypeManagement atMan;	
	private final CredentialManagement credMan;
	private final GroupsManagement groupsMan;
	private final EntityManagement idMan;	
	private final AttributesManagement attrMan;
	private final IdPLoginController idpLoginController;
	private final PolicyAgreementRepresentationBuilderV23 policyAgreementsRepresentationBuilder;
	private final PolicyAgreementManagement policyAgrMan;
	private final InvitationManagement invitationManagement;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	public EnquiryResponseEditorControllerV23(MessageSource msg,
	                                          @Qualifier("insecure") EnquiryManagement enquiryManagement,
	                                          IdentityEditorRegistryV23 identityEditorRegistry,
	                                          CredentialEditorRegistryV23 credentialEditorRegistry,
	                                          AttributeHandlerRegistryV23 attributeHandlerRegistry,
	                                          @Qualifier("insecure") AttributeTypeManagement atMan,
	                                          @Qualifier("insecure") CredentialManagement credMan,
	                                          @Qualifier("insecure") GroupsManagement groupsMan,
	                                          @Qualifier("insecure") EntityManagement idMan,
	                                          @Qualifier("insecure") AttributesManagement attrMan,
	                                          @Qualifier("insecure") InvitationManagement invitationMan,
	                                          IdPLoginController idpLoginController,
	                                          NotificationPresenter notificationPresenter,
	                                          PolicyAgreementRepresentationBuilderV23 policyAgreementsRepresentationBuilder,
	                                          PolicyAgreementManagement policyAgrMan)
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
	}

	private EnquiryResponseEditor getEditorInstance(EnquiryForm form, Map<String, Object> messageParams,
	                                                                                     RemotelyAuthenticatedPrincipal remoteContext, PrefilledSet set,
	                                                                                     List<PolicyAgreementConfiguration> filteredPolicyAgreement) throws Exception
	{
		return new EnquiryResponseEditor(msg, form, remoteContext,
				identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, atMan, credMan, groupsMan, notificationPresenter,
				policyAgreementsRepresentationBuilder, filteredPolicyAgreement, set, messageParams);
	}

	public EnquiryResponseEditor getEditorInstanceForUnauthenticatedUser(EnquiryForm form, Map<String, Object> messageParams,
	                                                                                                          RemotelyAuthenticatedPrincipal remoteContext, PrefilledSet prefilled,
	                                                                                                          EntityParam entityId) throws Exception
	{
		List<PolicyAgreementConfiguration> filteredPolicyAgreement = policyAgrMan.filterAgreementToPresent(
				entityId, form.getPolicyAgreements());
		return getEditorInstance(form, messageParams, remoteContext, prefilled, filteredPolicyAgreement);
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
			WebSession.getCurrent().getEventBus().fireEvent(new EnquiryResponseChangedEvent(requestId));
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
