/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.RegistrationMVELContext.ContextKey;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
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
public class GroupDelegationConfigGeneratorImpl implements GroupDelegationConfigGenerator
{
	private static final String REGISTRATION_NAME_SUFFIX = "Registration";
	private static final String JOIN_ENQUIRY_NAME_SUFFIX = "JoinEnquiry";
	private UnityMessageSource msg;
	private RegistrationFormDB regFormDB;
	private MessageTemplateDB messageDB;
	private EnquiryFormDB enqFormDB;
	private AttributesHelper attrHelper;

	@Autowired
	public GroupDelegationConfigGeneratorImpl(UnityMessageSource msg, RegistrationFormDB regFormDB,
			MessageTemplateDB messageDB, EnquiryFormDB enqFormDB, AttributesHelper attrHelper)
	{

		this.msg = msg;
		this.regFormDB = regFormDB;
		this.enqFormDB = enqFormDB;
		this.attrHelper = attrHelper;
		this.messageDB = messageDB;
	}

	@Transactional
	@Override
	public List<String> validateJoinEnquiryForm(String formName, String groupPath)
	{
		List<String> ret = new ArrayList<>();

		EnquiryForm form;

		try
		{
			form = enqFormDB.get(formName);

		} catch (Exception e)
		{
			ret.add(msg.getMessage("FormGenerator.invalidEnquiryForm"));
			return ret;
		}

		ret.addAll(validateAutomationProfile(form, groupPath));
		ret.addAll(validateNotifications(form));
		return ret;
	}

	@Transactional
	@Override
	public List<String> validateRegistrationForm(String formName, String groupPath)
	{
		List<String> ret = new ArrayList<>();

		RegistrationForm form;

		try
		{
			form = regFormDB.get(formName);

		} catch (Exception e)
		{
			ret.add(msg.getMessage("FormGenerator.invalidRegistrationForm"));
			return ret;
		}

		if (form.getIdentityParams() == null || form.getIdentityParams().size() != 1
				|| !form.getIdentityParams().get(0).getIdentityType().equals(EmailIdentity.ID))
		{
			ret.add(msg.getMessage("FormGenerator.noEmailIdentity"));
		}

		ret.addAll(validateAutomationProfile(form, groupPath));
		ret.addAll(validateNotifications(form));

		return ret;
	}

	private List<String> validateNotifications(BaseForm form)
	{
		List<String> ret = new ArrayList<>();
		BaseFormNotifications notConfig = form.getNotificationsConfiguration();
		if (notConfig == null || notConfig.getInvitationTemplate() == null
				|| notConfig.getInvitationTemplate().isEmpty())
		{
			ret.add(msg.getMessage("FormGenerator.noInvitationTemplate"));
		}
		return ret;
	}

	private List<String> validateAutomationProfile(BaseForm form, String group)
	{
		List<? extends TranslationRule> rules = form.getTranslationProfile().getRules();
		List<String> ret = new ArrayList<>();
		if (!rules.stream()
				.filter(r -> r.getCondition().contains(ContextKey.validCode.toString())
						&& r.getAction().getName().equals(AutoProcessActionFactory.NAME)
						&& !(r.getAction().getParameters().length == 0)
						&& r.getAction().getParameters()[0]
								.equals(AutomaticRequestAction.accept.toString()))
				.findFirst().isPresent())
		{
			ret.add(msg.getMessage("FormGenerator.noAutoAccept"));
		}

		if (!rules.stream()
				.filter(r -> r.getAction().getName().equals(AddToGroupActionFactory.NAME)
						&& !(r.getAction().getParameters().length == 0)
						&& r.getAction().getParameters()[0].contains(group))
				.findFirst().isPresent())
		{
			ret.add(msg.getMessage("FormGenerator.noAutoGroupAdd"));
		}
		return ret;

	}

	@Transactional
	@Override
	public RegistrationForm generateRegistrationForm(Group group, String logo, List<String> attributes)
			throws EngineException
	{

		Set<String> actualForms = regFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());

		RegistrationForm form = new RegistrationFormBuilder()
				.withName(generateName(REGISTRATION_NAME_SUFFIX, group, actualForms))
				.withNotificationsConfiguration(getDefaultRegistrationNotificationConfig())
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withAddedCredentialParam(new CredentialRegistrationParam(
						EngineInitialization.DEFAULT_CREDENTIAL, null, null))
				.withAddedIdentityParam().withIdentityType(EmailIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
				.endIdentityParam().withAddedGroupParam()
				.withLabel(msg.getMessage("FormGenerator.selectGroups"))
				.withGroupPath(group.toString() + "/?*/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive).withMultiselect(true)
				.endGroupParam().withFormLayoutSettings(getDefaultLayoutSettings(logo))
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.createAccount")))
				.withTitle2ndStage(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.provideDetails")))
				.withTranslationProfile(getAutomationProfile(group)).build();

		String nameAttr = getNameAttribute();
		if (nameAttr != null)
		{

			addAttributeParam(form.getAttributeParams(), nameAttr, "/", false);
		}

		for (String attribute : attributes)
		{
			addAttributeParam(form.getAttributeParams(), attribute, group.toString(), true);
		}

		return form;
	}

	@Transactional
	@Override
	public EnquiryForm generateJoinEnquiryForm(Group group, String logo) throws EngineException
	{

		Set<String> actualForms = enqFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());

		return new EnquiryFormBuilder().withName(generateName(JOIN_ENQUIRY_NAME_SUFFIX, group, actualForms))
				.withTargetGroups(new String[] { "/" }).withType(EnquiryForm.EnquiryType.STICKY)
				.withAddedGroupParam().withLabel(msg.getMessage("FormGenerator.selectGroups"))
				.withMultiselect(true).withGroupPath(group.toString() + "/?*/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive).endGroupParam()
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.updateAccount")))
				.withFormLayoutSettings(getDefaultLayoutSettings(logo))
				.withNotificationsConfiguration(getDefaultEnquiryNotificationConfig())
				.withTranslationProfile(getAutomationProfile(group)).build();
	}

	private RegistrationFormNotifications getDefaultRegistrationNotificationConfig()
	{
		RegistrationFormNotifications not = new RegistrationFormNotifications();
		not.setInvitationTemplate(getDefaultInvitationTemplate());
		return not;
	}

	private EnquiryFormNotifications getDefaultEnquiryNotificationConfig()
	{
		EnquiryFormNotifications not = new EnquiryFormNotifications();
		not.setInvitationTemplate(getDefaultInvitationTemplate());
		return not;
	}

	private String getDefaultInvitationTemplate()
	{
		return messageDB.getAll().stream().filter(m -> m.getConsumer().equals(InvitationTemplateDef.NAME))
				.map(m -> m.getName()).findAny().orElse(null);
	}

	private TranslationProfile getAutomationProfile(Group group)
	{
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME,
				new String[] { AutomaticRequestAction.accept.toString() });

		TranslationAction a2 = new TranslationAction(AddToGroupActionFactory.NAME,
				new String[] { "\"" + group.toString() + "\"" });

		List<TranslationRule> rules = Lists.newArrayList(
				new TranslationRule(ContextKey.validCode.toString() + " == true", a1),
				new TranslationRule("true", a2));

		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);

		return tp;
	}

	private FormLayoutSettings getDefaultLayoutSettings(String logo)
	{

		FormLayoutSettings lsettings = new FormLayoutSettings();
		lsettings.setLogoURL(logo);
		lsettings.setColumnWidth(21);
		lsettings.setColumnWidthUnit("em");
		return lsettings;
	}

	private String generateName(String suffix, Group group, Set<String> actualNames)
	{

		String name = group.getDisplayedName().getValue(msg) + suffix;
		String nextFreeName = new String(name);
		int i = 1;
		while (actualNames.contains(nextFreeName))
		{
			nextFreeName = name + i++;
		}
		return nextFreeName;
	}

	private void addAttributeParam(List<AttributeRegistrationParam> params, String name, String group,
			boolean optional)
	{
		AttributeRegistrationParam param = new AttributeRegistrationParam();
		param.setAttributeType(name);
		param.setGroup(group);
		param.setRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive);
		param.setOptional(optional);
		params.add(param);
	}

	private String getNameAttribute() throws EngineException
	{
		AttributeType attrType = attrHelper
				.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		if (attrType == null)
			return null;

		return attrType.getName();
	}
}
