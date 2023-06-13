/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.base.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.base.registration.layout.BasicFormElement;
import pl.edu.icm.unity.base.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.base.registration.layout.FormLayout;
import pl.edu.icm.unity.base.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.base.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.base.registration.layout.FormLocalSignupButtonElement;
import pl.edu.icm.unity.base.registration.layout.FormParameterElement;
import pl.edu.icm.unity.base.registration.layout.FormSeparatorElement;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.objstore.reg.common.DBAgreementRegistrationParamProvider;
import pl.edu.icm.unity.store.objstore.reg.common.DBAttributeRegistrationParamProvider;
import pl.edu.icm.unity.store.objstore.reg.common.DBCredentialRegistrationParamProvider;
import pl.edu.icm.unity.store.objstore.reg.common.DBGroupRegistrationParamProvider;
import pl.edu.icm.unity.store.objstore.reg.common.DBIdentityRegistrationParamProvider;
import pl.edu.icm.unity.store.objstore.reg.common.DBPolicyAgreementConfigurationProvider;
import pl.edu.icm.unity.store.objstore.reg.common.DBRegistrationWrapUpConfigProvider;
import pl.edu.icm.unity.store.objstore.reg.layout.DBBasicFormElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormCaptionElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLayout;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLayoutSettings;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormLocalSignupButtonElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormParameterElement;
import pl.edu.icm.unity.store.objstore.reg.layout.DBFormSeparatorElement;
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationAction;
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile;
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile.Condition;
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile.DBTranslationProfileRule;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class EnquiryFormMapperTest extends MapperWithMinimalTestBase<EnquiryForm, DBEnquiryForm>
{

	@Override
	protected EnquiryForm getFullAPIObject()
	{
		EnquiryFormNotifications enquiryFormNotifications = new EnquiryFormNotifications();
		enquiryFormNotifications.setAcceptedTemplate("acceptedTemplate");
		enquiryFormNotifications.setAdminsNotificationGroup("/group");
		enquiryFormNotifications.setInvitationProcessedTemplate("invitationProcessedTemplate");
		enquiryFormNotifications.setInvitationTemplate("invitationTemplate");
		enquiryFormNotifications.setRejectedTemplate("rejectTemplate");
		enquiryFormNotifications.setSendUserNotificationCopyToAdmin(true);
		enquiryFormNotifications.setSubmittedTemplate("submittedTemplate");
		enquiryFormNotifications.setUpdatedTemplate("updatedTemplate");
		enquiryFormNotifications.setEnquiryToFillTemplate("enquiryToFillTemplate");

		FormLayout formLayout = new FormLayout(List.of(new BasicFormElement(FormLayoutElement.REG_CODE),
				new FormCaptionElement(new I18nString("value")),
				new FormParameterElement(FormLayoutElement.ATTRIBUTE, 1), new FormSeparatorElement(),
				new FormLocalSignupButtonElement()));

		TranslationAction a1 = new TranslationAction("autoProcess", new String[]
		{ "accept" });
		List<TranslationRule> rules = List.of(new TranslationRule("true", a1));

		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);

		return new EnquiryFormBuilder().withName("f1")
				.withDescription("desc")
				.withTranslationProfile(tp)
				.withCollectComments(true)
				.withFormInformation(new I18nString("formInformation"))
				.withAddedCredentialParam(
						new CredentialRegistrationParam("sys:password", null, null))
				.withAddedAgreement(new AgreementRegistrationParam(new I18nString("a"), false))
				.withAddedIdentityParam()
				.withIdentityType("userName")
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
				.withAttributeType("email")
				.withGroup("/")
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.withShowGroups(true)
				.endAttributeParam()
				.withAddedGroupParam()
				.withGroupPath("/B")
				.withRetrievalSettings(ParameterRetrievalSettings.automatic)
				.endGroupParam()
				.withCollectComments(true)
				.withDisplayedName(new I18nString("displayedName"))
				.withFormInformation(new I18nString("formInfo"))
				.withPageTitle(new I18nString("pageTitle"))
				.withFormLayoutSettings(new FormLayoutSettings(true, 50, "em", true, "logoUrl"))
				.withWrapUpConfig(List.of(new RegistrationWrapUpConfig(TriggeringState.AUTO_ACCEPTED)))
				.withPolicyAgreements(List.of(new PolicyAgreementConfiguration(List.of(1L),
						PolicyAgreementPresentationType.CHECKBOX_SELECTED, new I18nString("text"))))
				.withByInvitationOnly(true)
				.withCheckIdentityOnSubmit(true)
				.withLayout(formLayout)
				.withType(EnquiryType.REQUESTED_MANDATORY)
				.withTargetGroups(new String[]
				{ "/" })
				.withTargetCondition("true")
				.withNotificationsConfiguration(enquiryFormNotifications)
				.build();

	}

	@Override
	protected DBEnquiryForm getFullDBObject()
	{
		return DBEnquiryForm.builder()
				.withName("f1")
				.withDescription("desc")
				.withTranslationProfile(DBTranslationProfile.builder()
						.withName("form")
						.withDescription("")
						.withType("REGISTRATION")
						.withMode("DEFAULT")
						.withRules(List.of(DBTranslationProfileRule.builder()
								.withCondition(Condition.builder()
										.withConditionValue("true")
										.build())
								.withAction(DBTranslationAction.builder()
										.withName("autoProcess")
										.withParameters(List.of("accept"))
										.build())
								.build()))
						.build())
				.withCollectComments(true)
				.withFormInformation(DBI18nString.builder()
						.withDefaultValue("formInformation")
						.build())
				.withCredentialParams(List.of(DBCredentialRegistrationParamProvider.getParam()))
				.withAgreements(List.of(DBAgreementRegistrationParamProvider.getRegistrationParam()))
				.withIdentityParams(List.of(DBIdentityRegistrationParamProvider.getParam()))
				.withAttributeParams(List.of(DBAttributeRegistrationParamProvider.getParam()))
				.withGroupParams(List.of(DBGroupRegistrationParamProvider.getParam()))
				.withCollectComments(true)
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("displayedName")
						.build())
				.withFormInformation(DBI18nString.builder()
						.withDefaultValue("formInfo")
						.build())
				.withPageTitle(DBI18nString.builder()
						.withDefaultValue("pageTitle")
						.build())
				.withLayoutSettings(DBFormLayoutSettings.builder()
						.withCompactInputs(true)
						.withColumnWidth(50)
						.withColumnWidthUnit("em")
						.withLogoURL("logoUrl")
						.withShowCancel(true)
						.build())
				.withWrapUpConfig(List.of(DBRegistrationWrapUpConfigProvider.getParam()))
				.withPolicyAgreements(List.of(DBPolicyAgreementConfigurationProvider.getParam()))
				.withByInvitationOnly(true)
				.withCheckIdentityOnSubmit(true)
				.withLayout(DBFormLayout.builder()
						.withElements(List.of(DBBasicFormElement.builder()
								.withType("REG_CODE")
								.build(),
								DBFormCaptionElement.builder()
										.withValue(DBI18nString.builder()
												.withDefaultValue("value")
												.build())
										.build(),
								DBFormParameterElement.builder()
										.withIndex(1)
										.withType("ATTRIBUTE")
										.build(),
								DBFormSeparatorElement.builder()
										.build(),
								DBFormLocalSignupButtonElement.builder()
										.build()))
						.build())
				.withType("REQUESTED_MANDATORY")
				.withTargetGroups(List.of("/"))
				.withTargetCondition("true")
				.withNotificationsConfiguration(DBEnquiryFormNotifications.builder()
						.withAcceptedTemplate("acceptedTemplate")
						.withEnquiryToFillTemplate("enquiryToFillTemplate")
						.withAdminsNotificationGroup("/group")
						.withInvitationProcessedTemplate("invitationProcessedTemplate")
						.withInvitationTemplate("invitationTemplate")
						.withRejectedTemplate("rejectTemplate")
						.withSendUserNotificationCopyToAdmin(true)
						.withUpdatedTemplate("updatedTemplate")
						.withSubmittedTemplate("submittedTemplate")
						.build())
				.build();
	}

	@Override
	protected EnquiryForm getMinAPIObject()
	{
		return new EnquiryFormBuilder().withName("f1")
				.withType(EnquiryType.REQUESTED_MANDATORY)
				.withTargetGroups(new String[]
				{ "/" })
				.build();
	}

	@Override
	protected DBEnquiryForm getMinDBObject()
	{
		return DBEnquiryForm.builder()
				.withName("f1")
				.withType("REQUESTED_MANDATORY")
				.withTargetGroups(List.of("/"))
				.build();
	}
	
	@Test
	public void shouldSupportStringFormInformation()
	{
		DBEnquiryForm restEnquiryForm = DBEnquiryForm.builder()
				.withName("f1")
				.withFormInformation("formInfo")
				.withType("REQUESTED_MANDATORY")
				.withTargetGroups(List.of("/"))
				.build();
		EnquiryForm map = EnquiryFormMapper.map(restEnquiryForm);
		assertThat(map.getFormInformation()
				.getDefaultValue()).isEqualTo("formInfo");
	}


	@Override
	protected Pair<Function<EnquiryForm, DBEnquiryForm>, Function<DBEnquiryForm, EnquiryForm>> getMapper()
	{
		return Pair.of(EnquiryFormMapper::map, EnquiryFormMapper::map);
	}

}
