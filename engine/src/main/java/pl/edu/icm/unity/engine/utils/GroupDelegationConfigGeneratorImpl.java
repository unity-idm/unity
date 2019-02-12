/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.NewEnquiryTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.UpdateRegistrationTemplateDef;
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
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
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
@Primary
public class GroupDelegationConfigGeneratorImpl implements GroupDelegationConfigGenerator
{
	private UnityMessageSource msg;
	private RegistrationFormDB regFormDB;
	private MessageTemplateDB messageDB;
	private EnquiryFormDB enqFormDB;
	private GroupDAO groupDB;
	private AttributesHelper attrHelper;

	@Autowired
	public GroupDelegationConfigGeneratorImpl(UnityMessageSource msg, RegistrationFormDB regFormDB,
			MessageTemplateDB messageDB, EnquiryFormDB enqFormDB, AttributesHelper attrHelper,
			GroupDAO groupDB)
	{

		this.msg = msg;
		this.regFormDB = regFormDB;
		this.enqFormDB = enqFormDB;
		this.attrHelper = attrHelper;
		this.messageDB = messageDB;
		this.groupDB = groupDB;
	}

	@Transactional
	@Override
	public List<String> validateJoinEnquiryForm(String groupPath, String formName)
	{
		List<String> ret = new ArrayList<>();

		EnquiryForm form = getEnquiryForm(formName);
		if (form == null)
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
	public List<String> validateUpdateEnquiryForm(String groupPath, String formName)
	{
		List<String> ret = new ArrayList<>();

		EnquiryForm form = getEnquiryForm(formName);
		if (form == null)
		{
			ret.add(msg.getMessage("FormGenerator.invalidEnquiryForm"));
			return ret;
		}

		if (!Arrays.asList(form.getTargetGroups()).contains(groupPath))
		{
			ret.add(msg.getMessage("FormGenerator.targetGroupWithoutProject"));
		}

		return ret;
	}

	private EnquiryForm getEnquiryForm(String formName)
	{
		try
		{
			return enqFormDB.get(formName);

		} catch (Exception e)
		{
			return null;
		}
	}

	@Transactional
	@Override
	public List<String> validateRegistrationForm(String groupPath, String formName)
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
		if (notConfig == null)
		{
			ret.add(msg.getMessage("FormGenerator.noInvitationTemplate"));
			ret.add(msg.getMessage("FormGenerator.noAcceptTemplate"));
			ret.add(msg.getMessage("FormGenerator.noRejectTemplate"));
			ret.add(msg.getMessage("FormGenerator.noUpdateTemplate"));
		} else
		{
			if (notConfig.getInvitationTemplate() == null || notConfig.getInvitationTemplate().isEmpty())
			{
				ret.add(msg.getMessage("FormGenerator.noInvitationTemplate"));
			}

			if (notConfig.getAcceptedTemplate() == null || notConfig.getInvitationTemplate().isEmpty())
			{
				ret.add(msg.getMessage("FormGenerator.noAcceptTemplate"));
			}

			if (notConfig.getRejectedTemplate() == null || notConfig.getInvitationTemplate().isEmpty())
			{
				ret.add(msg.getMessage("FormGenerator.noRejectTemplate"));
			}

			if (notConfig.getUpdatedTemplate() == null || notConfig.getInvitationTemplate().isEmpty())
			{
				ret.add(msg.getMessage("FormGenerator.noUpdateTemplate"));
			}
		}

		return ret;
	}

	private List<String> validateAutomationProfile(BaseForm form, String group)
	{
		List<? extends TranslationRule> rules = form.getTranslationProfile().getRules();
		List<String> ret = new ArrayList<>();
		if (!rules.stream()
				.filter(r -> r.getAction().getName().equals(AutoProcessActionFactory.NAME)
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
	public RegistrationForm generateRegistrationForm(String groupPath, String logo, List<String> attributes)
			throws EngineException
	{

		Set<String> actualForms = regFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
		String groupDisplayedName = getGroupDisplayedName(groupPath);

		RegistrationForm form = new RegistrationFormBuilder()
				.withName(generateName(msg.getMessage("FormGenerator.registrationNameSuffix"),
						groupDisplayedName, actualForms))
				.withNotificationsConfiguration(getDefaultRegistrationNotificationConfig())
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withAddedCredentialParam(new CredentialRegistrationParam(
						EngineInitialization.DEFAULT_CREDENTIAL, null, null))
				.withAddedIdentityParam().withIdentityType(EmailIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
				.endIdentityParam()
				.withAddedIdentityParam().withIdentityType(IdentifierIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.withOptional(true)
				.endIdentityParam()
				.withAddedGroupParam()
				.withLabel(msg.getMessage("FormGenerator.selectGroups"))
				.withGroupPath(groupPath + "/?*/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive).withMultiselect(true)
				.endGroupParam().withFormLayoutSettings(getDefaultLayoutSettings(logo))
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.joinTitle", groupDisplayedName)))
				.withTitle2ndStage(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.provideDetails")))
				.withTranslationProfile(getAutomationProfile(groupPath)).build();

		String nameAttr = getNameAttribute();
		if (nameAttr != null)
		{

			addAttributeParam(form.getAttributeParams(), nameAttr, "/", false);
		}

		for (String attribute : attributes)
		{
			addAttributeParam(form.getAttributeParams(), attribute, groupPath, true);
		}

		return form;
	}

	@Transactional
	@Override
	public EnquiryForm generateJoinEnquiryForm(String groupPath, String logo) throws EngineException
	{

		Set<String> actualForms = enqFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
		String groupDisplayedName = getGroupDisplayedName(groupPath);

		return new EnquiryFormBuilder()
				.withName(generateName(msg.getMessage("FormGenerator.joinEnquiryNameSuffix"),
						groupDisplayedName, actualForms))
				.withTargetGroups(new String[] { "/" }).withType(EnquiryForm.EnquiryType.STICKY)
				.withTargetCondition("!(groups contains '" + groupPath + "')")
				.withAddedGroupParam().withLabel(msg.getMessage("FormGenerator.selectGroups"))
				.withMultiselect(true).withGroupPath(groupPath + "/?*/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive).endGroupParam()
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.joinTitle", groupDisplayedName)))
				.withFormLayoutSettings(getDefaultLayoutSettings(logo))
				.withNotificationsConfiguration(getDefaultEnquiryNotificationConfig())
				.withTranslationProfile(getAutomationProfile(groupPath)).build();
	}

	@Transactional
	@Override
	public EnquiryForm generateUpdateEnquiryForm(String groupPath, String logo) throws EngineException
	{

		Set<String> actualForms = enqFormDB.getAll().stream().map(r -> r.getName()).collect(Collectors.toSet());
		String groupDisplayedName = getGroupDisplayedName(groupPath);

		return new EnquiryFormBuilder()
				.withName(generateName(msg.getMessage("FormGenerator.updateEnquiryNameSuffix"),
						groupDisplayedName, actualForms))
				.withTargetGroups(new String[] { groupPath }).withType(EnquiryForm.EnquiryType.STICKY)
				.withAddedGroupParam().withLabel(msg.getMessage("FormGenerator.selectGroups"))
				.withMultiselect(true).withGroupPath(groupPath + "/?*/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive).endGroupParam()
				.withDisplayedName(new I18nString(msg.getLocaleCode(),
						msg.getMessage("FormGenerator.updateTitle",
								groupDisplayedName)))
				.withFormLayoutSettings(getDefaultLayoutSettings(logo))
				.build();
	}

	private RegistrationFormNotifications getDefaultRegistrationNotificationConfig()
	{
		RegistrationFormNotifications not = new RegistrationFormNotifications();
		not.setInvitationTemplate(getDefaultInvitationTemplate());
		not.setInvitationTemplate(getDefaultInvitationTemplate());
		not.setAcceptedTemplate(getDefaultAcceptTemplate());
		not.setRejectedTemplate(getDefaultRejectTemplate());
		not.setUpdatedTemplate(getDefaultUpdateTemplate());
		return not;
	}

	private EnquiryFormNotifications getDefaultEnquiryNotificationConfig()
	{
		EnquiryFormNotifications not = new EnquiryFormNotifications();
		not.setInvitationTemplate(getDefaultInvitationTemplate());
		not.setAcceptedTemplate(getDefaultAcceptTemplate());
		not.setRejectedTemplate(getDefaultRejectTemplate());
		not.setUpdatedTemplate(getDefaultUpdateTemplate());
		not.setEnquiryToFillTemplate(getDefaultNewEnquiryTemplate());		
		return not;
	}

	
	private String getDefaultInvitationTemplate()
	{	
		return  getDefaultMessageTemplate(InvitationTemplateDef.NAME);
	}
	
	private String getDefaultRejectTemplate()
	{
		return  getDefaultMessageTemplate(RejectRegistrationTemplateDef.NAME);
	}
	
	private String getDefaultAcceptTemplate()
	{
		return  getDefaultMessageTemplate(AcceptRegistrationTemplateDef.NAME);
	}
	
	private String getDefaultUpdateTemplate()
	{
		return  getDefaultMessageTemplate(UpdateRegistrationTemplateDef.NAME);
	}
	
	private String getDefaultNewEnquiryTemplate()
	{
		return  getDefaultMessageTemplate(NewEnquiryTemplateDef.NAME);
	}	
	
	private String getDefaultMessageTemplate(String type)
	{
		return messageDB.getAll().stream().filter(m -> m.getConsumer().equals(type))
				.map(m -> m.getName()).findAny().orElse(null);
	}

	private TranslationProfile getAutomationProfile(String group)
	{
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME,
				new String[] { AutomaticRequestAction.accept.toString() });

		TranslationAction a2 = new TranslationAction(AddToGroupActionFactory.NAME,
				new String[] { "\"" + group + "\"" });

		List<TranslationRule> rules = Lists.newArrayList(
				new TranslationRule(ContextKey.validCode.toString() + " == true", a1),
				new TranslationRule("true", a2));

		TranslationProfile tp = new TranslationProfile("autoProfile" , "", ProfileType.REGISTRATION, rules);

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

	private String getGroupDisplayedName(String group)
	{
		return groupDB.get(group).getDisplayedName().getValue(msg);
	}

	private String generateName(String suffix, String group, Set<String> actualNames)
	{
		String newName = group + suffix;
		String nextFreeName = newName;
		int i = 1;
		while (actualNames.contains(nextFreeName))
		{
			nextFreeName = newName + i++;
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
