/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.NewEnquiryTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.PolicyDocumentDAO;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.exceptions.EntityNotFoundException;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
@Primary
public class GroupDelegationConfigGeneratorImpl implements GroupDelegationConfigGenerator {
	private final MessageSource msg;
	private final RegistrationFormDB regFormDB;
	private final MessageTemplateDB messageDB;
	private final EnquiryFormDB enqFormDB;
	private final GroupDAO groupDB;
	private final AttributesHelper attrHelper;
	private final PolicyDocumentDAO policyDocumentDB;

	@Autowired
	public GroupDelegationConfigGeneratorImpl(MessageSource msg, RegistrationFormDB regFormDB,
			MessageTemplateDB messageDB, EnquiryFormDB enqFormDB, AttributesHelper attrHelper, GroupDAO groupDB,
			PolicyDocumentDAO policyDocumentDB) {

		this.msg = msg;
		this.regFormDB = regFormDB;
		this.enqFormDB = enqFormDB;
		this.attrHelper = attrHelper;
		this.messageDB = messageDB;
		this.groupDB = groupDB;
		this.policyDocumentDB = policyDocumentDB;
	}

	@Transactional
	@Override
	public List<String> validateJoinEnquiryForm(String groupPath, String formName,
			Set<Long> projectPolicyDocumentsIds) {
		List<String> ret = new ArrayList<>();

		EnquiryForm form = getEnquiryForm(formName);
		if (form == null) {
			ret.add(msg.getMessage("FormGenerator.invalidEnquiryForm"));
			return ret;
		}

		ret.addAll(validateAutomationProfile(form, groupPath));
		ret.addAll(validateNotifications(form));
		ret.addAll(validatePolicies(form, projectPolicyDocumentsIds));
		return ret;
	}

	@Transactional
	@Override
	public List<String> validateUpdateEnquiryForm(String groupPath, String formName) {
		List<String> ret = new ArrayList<>();

		EnquiryForm form = getEnquiryForm(formName);
		if (form == null) {
			ret.add(msg.getMessage("FormGenerator.invalidEnquiryForm"));
			return ret;
		}

		if (!Arrays.asList(form.getTargetGroups()).contains(groupPath)) {
			ret.add(msg.getMessage("FormGenerator.targetGroupWithoutProject"));
		}

		return ret;
	}

	private EnquiryForm getEnquiryForm(String formName) {
		try {
			return enqFormDB.get(formName);

		} catch (Exception e) {
			return null;
		}
	}

	@Transactional
	@Override
	public List<String> validateRegistrationForm(String groupPath, String formName, Set<Long> projectPolicyDocumentsIds) {
		List<String> ret = new ArrayList<>();

		RegistrationForm form;

		try {
			form = regFormDB.get(formName);

		} catch (Exception e) {
			ret.add(msg.getMessage("FormGenerator.invalidRegistrationForm"));
			return ret;
		}

		if (form.getIdentityParams() == null || form.getIdentityParams().isEmpty()
				|| !form.getIdentityParams().get(0).getIdentityType().equals(EmailIdentity.ID)) {
			ret.add(msg.getMessage("FormGenerator.noEmailIdentity"));
		}

		ret.addAll(validateAutomationProfile(form, groupPath));
		ret.addAll(validateNotifications(form));
		ret.addAll(validatePolicies(form, projectPolicyDocumentsIds));

		return ret;
	}

	private List<String> validateNotifications(BaseForm form) {
		List<String> ret = new ArrayList<>();
		BaseFormNotifications notConfig = form.getNotificationsConfiguration();
		if (notConfig == null) {
			ret.add(msg.getMessage("FormGenerator.noInvitationTemplate"));
			ret.add(msg.getMessage("FormGenerator.noAcceptTemplate"));
			ret.add(msg.getMessage("FormGenerator.noRejectTemplate"));
			ret.add(msg.getMessage("FormGenerator.noUpdateTemplate"));
		} else {
			if (notConfig.getInvitationTemplate() == null || notConfig.getInvitationTemplate().isEmpty()) {
				ret.add(msg.getMessage("FormGenerator.noInvitationTemplate"));
			}

			if (notConfig.getAcceptedTemplate() == null || notConfig.getInvitationTemplate().isEmpty()) {
				ret.add(msg.getMessage("FormGenerator.noAcceptTemplate"));
			}

			if (notConfig.getRejectedTemplate() == null || notConfig.getInvitationTemplate().isEmpty()) {
				ret.add(msg.getMessage("FormGenerator.noRejectTemplate"));
			}

			if (notConfig.getUpdatedTemplate() == null || notConfig.getInvitationTemplate().isEmpty()) {
				ret.add(msg.getMessage("FormGenerator.noUpdateTemplate"));
			}
		}

		return ret;
	}

	private List<String> validateAutomationProfile(BaseForm form, String group) {
		List<? extends TranslationRule> rules = form.getTranslationProfile().getRules();
		List<String> ret = new ArrayList<>();
		if (!rules.stream()
				.filter(r -> r.getAction().getName().equals(AutoProcessActionFactory.NAME)
						&& !(r.getAction().getParameters().length == 0)
						&& r.getAction().getParameters()[0].equals(AutomaticRequestAction.accept.toString()))
				.findFirst().isPresent()) {
			ret.add(msg.getMessage("FormGenerator.noAutoAccept"));
		}

		if (!rules.stream()
				.filter(r -> r.getAction().getName().equals(AddToGroupActionFactory.NAME)
						&& !(r.getAction().getParameters().length == 0)
						&& r.getAction().getParameters()[0].contains(group))
				.findFirst().isPresent()) {
			ret.add(msg.getMessage("FormGenerator.noAutoGroupAdd"));
		}
		return ret;

	}

	private List<String> validatePolicies(BaseForm form, Set<Long> projectPolicyDocumentsIds) {

		Set<Long> formsPolicices = form.getPolicyAgreements().stream().map(p -> p.documentsIdsToAccept)
				.flatMap(Collection::stream).collect(Collectors.toSet());
		List<String> ret = new ArrayList<>();
		List<StoredPolicyDocument> allPolicies = policyDocumentDB.getAll();

		Set<Long> common = new HashSet<>(formsPolicices);
		common.retainAll(projectPolicyDocumentsIds);

		if (projectPolicyDocumentsIds.size() > common.size()) {
			List<String> collect = projectPolicyDocumentsIds.stream().filter(p -> !common.contains(p))
					.map(p -> allPolicies.stream().filter(pd -> pd.getId().equals(p)).findFirst()
							.orElse(new StoredPolicyDocument(p, p.toString())))
					.map(p -> p.getName()).collect(Collectors.toList());
			ret.add(msg.getMessage("FormGenerator.missingFormPolicies", String.join(", ", collect)));
		}

		if (formsPolicices.size() > common.size()) {
			List<String> collect = formsPolicices.stream().filter(p -> !common.contains(p))
					.map(p -> allPolicies.stream().filter(pd -> pd.getId().equals(p)).findFirst()
							.orElse(new StoredPolicyDocument(p, p.toString())))
					.map(p -> p.getName()).collect(Collectors.toList());
			ret.add(msg.getMessage("FormGenerator.additionalFormPolicies", String.join(", ", collect)));
		}

		return ret;

	}

	@Transactional
	@Override
	public RegistrationForm generateProjectRegistrationForm(String groupPath, String logo, List<String> attributes,
			List<Long> policyDocumentsIds) throws EngineException {

		Set<String> actualForms = regFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
		String groupDisplayedName = getGroupDisplayedName(groupPath);

		RegistrationForm form = new RegistrationFormBuilder()
				.withName(generateName(msg.getMessage("FormGenerator.registrationNameSuffix"), groupDisplayedName,
						actualForms))
				.withNotificationsConfiguration(getDefaultRegistrationNotificationConfig())
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withAddedCredentialParam(
						new CredentialRegistrationParam(EngineInitialization.DEFAULT_CREDENTIAL, null, null))
				.withAddedIdentityParam().withIdentityType(EmailIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive).endIdentityParam()
				.withAddedIdentityParam().withIdentityType(IdentifierIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden).withOptional(true).endIdentityParam()
				.withAddedGroupParam().withLabel(msg.getMessage("FormGenerator.yourGroups"))
				.withGroupPath(groupPath + "/?*/**").withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.withMultiselect(true).endGroupParam().withFormLayoutSettings(getDefaultLayoutSettings(logo))
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.joinTitle", groupDisplayedName)))
				.withTitle2ndStage(new I18nString(msg.getLocaleCode(), msg.getMessage("FormGenerator.provideDetails")))
				.withTranslationProfile(getAutomationProfile(groupPath)).build();

		String nameAttr = getNameAttribute();
		if (nameAttr != null) {

			addAttributeParam(form.getAttributeParams(), nameAttr, "/", false);
		}

		for (String attribute : attributes) {
			addAttributeParam(form.getAttributeParams(), attribute, groupPath, true);
		}

		for (Long policyDocumentId : policyDocumentsIds) {
			addPolicyParam(form.getPolicyAgreements(), policyDocumentId);
		}

		return form;
	}

	@Transactional
	@Override
	public EnquiryForm generateProjectJoinEnquiryForm(String groupPath, String logo, List<Long> policyDocuments)
			throws EngineException {

		Set<String> actualForms = enqFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
		String groupDisplayedName = getGroupDisplayedName(groupPath);

		EnquiryForm form = new EnquiryFormBuilder()
				.withName(generateName(msg.getMessage("FormGenerator.joinEnquiryNameSuffix"), groupDisplayedName,
						actualForms))
				.withTargetGroups(new String[] { "/" }).withType(EnquiryForm.EnquiryType.STICKY)
				.withTargetCondition("!(groups contains '" + groupPath + "')").withAddedGroupParam()
				.withLabel(msg.getMessage("FormGenerator.yourGroups")).withMultiselect(true)
				.withGroupPath(groupPath + "/?*/**").withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.joinTitle", groupDisplayedName)))
				.withFormLayoutSettings(getDefaultLayoutSettings(logo))
				.withNotificationsConfiguration(getDefaultEnquiryNotificationConfig())
				.withTranslationProfile(getAutomationProfile(groupPath)).build();

		for (Long document : policyDocuments) {
			addPolicyParam(form.getPolicyAgreements(), document);
		}

		return form;

	}

	@Transactional
	@Override
	public EnquiryForm generateProjectUpdateEnquiryForm(String groupPath, String logo) throws EngineException {

		Set<String> actualForms = enqFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
		String groupDisplayedName = getGroupDisplayedName(groupPath);

		return new EnquiryFormBuilder()
				.withName(generateName(msg.getMessage("FormGenerator.updateEnquiryNameSuffix"), groupDisplayedName,
						actualForms))
				.withTargetGroups(new String[] { groupPath }).withType(EnquiryForm.EnquiryType.STICKY)
				.withAddedGroupParam().withLabel(msg.getMessage("FormGenerator.yourGroups")).withMultiselect(true)
				.withGroupPath(groupPath + "/?*/**").withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.updateTitle", groupDisplayedName)))
				.withFormLayoutSettings(getDefaultLayoutSettings(logo)).build();
	}

	@Transactional
	@Override
	public RegistrationForm generateSubprojectRegistrationForm(String toCopyName, String projectPath,
			String subprojectPath, String logo) {
		Set<String> actualForms = regFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
		String groupDisplayedName = getGroupDisplayedName(subprojectPath);
		RegistrationForm toCopy = regFormDB.get(toCopyName);

		FormLayoutSettings formLayoutSettings = toCopy.getLayoutSettings();
		formLayoutSettings.setLogoURL(logo);

		return new RegistrationFormBuilder(toCopy)
				.withName(generateName(msg.getMessage("FormGenerator.registrationNameSuffix"), groupDisplayedName,
						actualForms))
				.withGroupParams(updateGroupParams(toCopy.getGroupParams(), projectPath, subprojectPath))
				.withFormLayoutSettings(formLayoutSettings)
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.joinTitle", groupDisplayedName)))
				.withTranslationProfile(
						updateTranslationProfile(toCopy.getTranslationProfile(), projectPath, subprojectPath))
				.build();
	}

	@Transactional
	@Override
	public EnquiryForm generateSubprojectJoinEnquiryForm(String toCopyName, String projectPath, String subprojectPath,
			String logo) {

		Set<String> actualForms = enqFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
		String groupDisplayedName = getGroupDisplayedName(subprojectPath);
		EnquiryForm toCopy = enqFormDB.get(toCopyName);
		FormLayoutSettings formLayoutSettings = toCopy.getLayoutSettings();
		formLayoutSettings.setLogoURL(logo);
		return new EnquiryFormBuilder(toCopy)
				.withName(generateName(msg.getMessage("FormGenerator.joinEnquiryNameSuffix"), groupDisplayedName,
						actualForms))
				.withTargetGroups(toCopy.getTargetGroups())
				.withTargetCondition("!(groups contains '" + subprojectPath + "')")
				.withGroupParams(updateGroupParams(toCopy.getGroupParams(), projectPath, subprojectPath))

				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.joinTitle", groupDisplayedName)))
				.withFormLayoutSettings(formLayoutSettings).withTranslationProfile(
						updateTranslationProfile(toCopy.getTranslationProfile(), projectPath, subprojectPath))
				.build();
	}

	@Transactional
	@Override
	public EnquiryForm generateSubprojectUpdateEnquiryForm(String toCopyName, String projectPath, String subprojectPath,
			String logo) {

		Set<String> actualForms = enqFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
		String groupDisplayedName = getGroupDisplayedName(subprojectPath);
		EnquiryForm toCopy = enqFormDB.get(toCopyName);
		FormLayoutSettings formLayoutSettings = toCopy.getLayoutSettings();
		formLayoutSettings.setLogoURL(logo);

		return new EnquiryFormBuilder(toCopy)
				.withName(generateName(msg.getMessage("FormGenerator.updateEnquiryNameSuffix"), groupDisplayedName,
						actualForms))
				.withTargetGroups(new String[] { subprojectPath })
				.withGroupParams(updateGroupParams(toCopy.getGroupParams(), projectPath, subprojectPath))
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.updateTitle", groupDisplayedName)))
				.withFormLayoutSettings(formLayoutSettings).build();
	}
	
	@Transactional
	@Override
	public void resetFormsPolicies(String formName, FormType formType, List<Long> projectPolicyDocumentsIds) throws EngineException {
		if (formType.equals(FormType.REGISTRATION))
		{
			RegistrationForm form = regFormDB.get(formName);
			form.getPolicyAgreements().clear();
			for (Long policyDocumentId : projectPolicyDocumentsIds) {
				addPolicyParam(form.getPolicyAgreements(), policyDocumentId);
			}
			regFormDB.update(form);
		}
		else if (formType.equals(FormType.ENQUIRY))
		{
			EnquiryForm form = enqFormDB.get(formName);
			form.getPolicyAgreements().clear();
			for (Long policyDocumentId : projectPolicyDocumentsIds) {
				addPolicyParam(form.getPolicyAgreements(), policyDocumentId);
			}
			enqFormDB.update(form);
		}
		
		
		
	}
	
	private List<GroupRegistrationParam> updateGroupParams(List<GroupRegistrationParam> toUpdate, String projectPath,
			String subprojectPath) {
		List<GroupRegistrationParam> groupParams = new ArrayList<>();
		for (GroupRegistrationParam groupParam : toUpdate) {
			GroupRegistrationParam newGroupParam = new GroupRegistrationParam();
			newGroupParam.setDescription(groupParam.getDescription());
			newGroupParam.setIncludeGroupsMode(groupParam.getIncludeGroupsMode());
			newGroupParam.setGroupPath(groupParam.getGroupPath());
			newGroupParam.setLabel(groupParam.getLabel());
			newGroupParam.setRetrievalSettings(groupParam.getRetrievalSettings());
			newGroupParam.setMultiSelect(groupParam.isMultiSelect());

			if (newGroupParam.getGroupPath().equals(projectPath + "/?*/**")) {
				newGroupParam.setGroupPath(subprojectPath + "/?*/**");
			} else if (newGroupParam.getGroupPath().equals(projectPath)) {
				newGroupParam.setGroupPath(subprojectPath);
			}
			groupParams.add(newGroupParam);
		}
		return groupParams;
	}

	private TranslationProfile updateTranslationProfile(TranslationProfile toUpdate, String projectPath,
			String subprojectPath) {
		List<TranslationRule> rules = new ArrayList<>();
		for (TranslationRule rule : toUpdate.getRules()) {
			if (rule.getAction().getName().equals(AddToGroupActionFactory.NAME)) {
				String[] params = rule.getAction().getParameters();
				for (int i = 0; i < params.length; i++) {
					params[i] = params[i].replace(projectPath, subprojectPath);
				}

				TranslationRule nrule = new TranslationRule(rule.getCondition(),
						new TranslationAction(rule.getAction().getName(), params));
				rules.add(nrule);
			} else {
				rules.add(rule);
			}
		}

		return new TranslationProfile("autoProfile", "", ProfileType.REGISTRATION, rules);
	}

	private RegistrationFormNotifications getDefaultRegistrationNotificationConfig() {
		RegistrationFormNotifications not = new RegistrationFormNotifications();
		not.setInvitationTemplate(getDefaultInvitationTemplate());
		not.setAcceptedTemplate(getDefaultAcceptTemplate());
		not.setRejectedTemplate(getDefaultRejectTemplate());
		not.setUpdatedTemplate(getDefaultUpdateTemplate());
		return not;
	}

	private EnquiryFormNotifications getDefaultEnquiryNotificationConfig() {
		EnquiryFormNotifications not = new EnquiryFormNotifications();
		not.setInvitationTemplate(getDefaultInvitationTemplate());
		not.setAcceptedTemplate(getDefaultAcceptTemplate());
		not.setRejectedTemplate(getDefaultRejectTemplate());
		not.setUpdatedTemplate(getDefaultUpdateTemplate());
		not.setEnquiryToFillTemplate(getDefaultNewEnquiryTemplate());
		return not;
	}

	private String getDefaultInvitationTemplate() {
		return getDefaultMessageTemplate(InvitationTemplateDef.NAME);
	}

	private String getDefaultRejectTemplate() {
		return getDefaultMessageTemplate(RejectRegistrationTemplateDef.NAME);
	}

	private String getDefaultAcceptTemplate() {
		return getDefaultMessageTemplate(AcceptRegistrationTemplateDef.NAME);
	}

	private String getDefaultUpdateTemplate() {
		return getDefaultMessageTemplate(UpdateRegistrationTemplateDef.NAME);
	}

	private String getDefaultNewEnquiryTemplate() {
		return getDefaultMessageTemplate(NewEnquiryTemplateDef.NAME);
	}

	private String getDefaultMessageTemplate(String type) {
		return messageDB.getAll().stream().filter(m -> m.getConsumer().equals(type)).map(m -> m.getName()).findAny()
				.orElse(null);
	}

	private TranslationProfile getAutomationProfile(String group) {
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME,
				new String[] { AutomaticRequestAction.accept.toString() });

		TranslationAction a2 = new TranslationAction(AddToGroupActionFactory.NAME,
				new String[] { "\"" + group + "\"" });

		List<TranslationRule> rules = Lists.newArrayList(
				new TranslationRule(RegistrationMVELContextKey.validCode.toString() + " == true", a1),
				new TranslationRule("true", a2));

		TranslationProfile tp = new TranslationProfile("autoProfile", "", ProfileType.REGISTRATION, rules);

		return tp;
	}

	private FormLayoutSettings getDefaultLayoutSettings(String logo) {

		FormLayoutSettings lsettings = new FormLayoutSettings();
		lsettings.setLogoURL(logo);
		lsettings.setColumnWidth(21);
		lsettings.setColumnWidthUnit("em");
		return lsettings;
	}

	private String getGroupDisplayedName(String groupName) {
		Group group;
		try {
			group = groupDB.get(groupName);
		} catch (EntityNotFoundException e) {
			throw new GroupNotFoundException(e.getMessage());
		}
		return group.getDisplayedName().getValue(msg);
	}

	private String generateName(String suffix, String group, Set<String> actualNames) {
		String newName = group + suffix;
		String nextFreeName = newName;
		int i = 1;
		while (actualNames.contains(nextFreeName)) {
			nextFreeName = newName + i++;
		}
		return nextFreeName;
	}

	private void addAttributeParam(List<AttributeRegistrationParam> params, String name, String group,
			boolean optional) {
		AttributeRegistrationParam param = new AttributeRegistrationParam();
		param.setAttributeType(name);
		param.setGroup(group);
		param.setRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive);
		param.setOptional(optional);
		params.add(param);
	}

	private void addPolicyParam(List<PolicyAgreementConfiguration> params, Long policyDocumentId) throws EngineException
	{
		StoredPolicyDocument policyDocument = policyDocumentDB.getByKey(policyDocumentId);
		PolicyAgreementConfiguration param = new PolicyAgreementConfiguration(List.of(policyDocument.getId()),
				PolicyAgreementPresentationType.CHECKBOX_NOTSELECTED,
				new I18nString(msg.getLocaleCode(), msg.getMessage("FormGenerator.policyAgreementText",
						"{" + policyDocument.getId() + ":" + policyDocument.getName() + "}")));
		params.add(param);
	}

	private String getNameAttribute() throws EngineException {
		AttributeType attrType = attrHelper.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		if (attrType == null)
			return null;

		return attrType.getName();
	}
}
