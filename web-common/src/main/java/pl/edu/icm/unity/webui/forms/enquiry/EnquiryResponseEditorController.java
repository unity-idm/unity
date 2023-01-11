/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

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

import pl.edu.icm.unity.MessageSource;
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
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector.AccessMode;
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector.Type;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IdentityExistsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementRepresentationBuilder;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.RegCodeException;

/**
 * Logic behind {@link EnquiryResponseEditor}. Provides a simple method to create editor instance and to handle 
 * response submission.
 * 
 * @author K. Benedyczak
 */
@Component
public class EnquiryResponseEditorController
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
	private final ImageAccessService imageAccessService;
	private final PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder;
	private final PolicyAgreementManagement policyAgrMan;
	private final InvitationManagement invitationManagement;

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
			ImageAccessService imageAccessService,
			PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder,
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
		this.imageAccessService = imageAccessService;
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
				attributeHandlerRegistry, atMan, credMan, groupsMan, imageAccessService, 
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
	
	public PrefilledSet getPrefilledSetForSticky(EnquiryForm form) throws EngineException, RegCodeException
	{
		return getPrefilledSetForSticky(form, getLoggedEntity());
	}

	public PrefilledSet getPrefilledSetForSticky(EnquiryForm form, EntityParam entity) throws EngineException, RegCodeException
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

			PrefilledEntry<GroupSelection> pe = new PrefilledEntry<GroupSelection>(new GroupSelection(
					filterMatching.stream().map(g -> g.getName()).collect(Collectors.toList())),
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
							&& a.getName().equals(attrParam.getAttributeType()))
					.collect(Collectors.toList());

			prefilledAttributes.put(i, !attributes.isEmpty() ? (new PrefilledEntry<>(
					  attributes.iterator().next(),
					PrefilledEntryMode.DEFAULT)) : null);
	
		}

		return prefilledAttributes;
	}
	
	public boolean isFormApplicableForLoggedEntity(String formName, boolean ignoreByIvitationForms)
	{
		EntityParam entity = getLoggedEntity();
		try
		{
			return enquiryManagement.getAvailableEnquires(entity, EnquirySelector.builder()
					.withType(Type.ALL)
					.withAccessMode(ignoreByIvitationForms ? AccessMode.NON_BY_INVITATION_ONLY : AccessMode.ANY)
					.build()).stream().filter(f -> f.getName().equals(formName)).findAny().isPresent();
		} catch (EngineException e)
		{
			log.error("Can't load enquiry forms", e);
		}

		return false;
	}

	public boolean isStickyFormApplicable(String formName)
	{
		EntityParam entity = getLoggedEntity();
		try
		{
			return enquiryManagement.getAvailableEnquires(entity, EnquirySelector.builder()
					.withAccessMode(AccessMode.NON_BY_INVITATION_ONLY)
					.withType(Type.STICKY)
					.build()).stream().filter(f -> f.getName().equals(formName)).findAny().isPresent();
		} catch (EngineException e)
		{
			log.error("Can't load sticky enquiry forms", e);
		}
		return false;
	}
	
	private EntityParam getLoggedEntity()
	{
		return  new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId());
	}
	
	public List<EnquiryForm> getRegularFormsToFill()
	{
		EntityParam entity = getLoggedEntity();
		try
		{
			return enquiryManagement.getAvailableEnquires(entity, EnquirySelector.builder()
					.withAccessMode(AccessMode.NON_BY_INVITATION_ONLY)
					.withType(Type.REGULAR)
					.build());
		} catch (EngineException e)
		{
			log.error("Can't load pending enquiry forms", e);
		}
		return Collections.emptyList();

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
	
	public EnquiryForm getForm(String name)
	{
		try
		{
			List<EnquiryForm> forms = enquiryManagement.getEnquires();
			for (EnquiryForm regForm: forms)
				if (regForm.getName().equals(name))
					return regForm;
		} catch (EngineException e)
		{
			log.error("Can't load enquiry forms", e);
		}
		return null;
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

	
	public boolean checkIfRequestExistsForLoggedUser(String formName) throws EngineException
	{
		long entityId = getLoggedEntity().getEntityId();	
		return checkIfRequestExists(formName, entityId);
	}
	
	boolean checkIfRequestExists(String formName, long entity) throws EngineException
	{
		return !enquiryManagement.getEnquiryResponses().stream()
				.filter(r -> r.getRequest().getFormId().equals(formName)
						&& r.getEntityId() == entity
						&& r.getStatus().equals(RegistrationRequestStatus.pending))
				.collect(Collectors.toList()).isEmpty();
	}
	
	public void removePendingRequest(String form) throws EngineException
	{
		enquiryManagement.removePendingStickyRequest(form, getLoggedEntity());	
	}
}
