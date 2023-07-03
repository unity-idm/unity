/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.base.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.base.registration.ExternalSignupGridSpec.AuthnGridSettings;
import pl.edu.icm.unity.base.registration.ExternalSignupSpec;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationFormLayouts;
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

public class RestRegistrationFormMapperTest extends MapperWithMinimalTestBase<RegistrationForm, DBRegistrationForm>
{

	@Override
	protected RegistrationForm getFullAPIObject()
	{
		FormLayout formLayout = new FormLayout(List.of(new BasicFormElement(FormLayoutElement.REG_CODE),
				new FormCaptionElement(new I18nString("value")),
				new FormParameterElement(FormLayoutElement.ATTRIBUTE, 1), new FormSeparatorElement(),
				new FormLocalSignupButtonElement()));

		FormLayout formLayout2 = new FormLayout(List.of(new BasicFormElement(FormLayoutElement.ATTRIBUTE),
				new FormCaptionElement(new I18nString("value2")),
				new FormParameterElement(FormLayoutElement.ATTRIBUTE, 1), new FormSeparatorElement(),
				new FormLocalSignupButtonElement()));

		RegistrationFormLayouts registrationFormLayouts = new RegistrationFormLayouts();
		registrationFormLayouts.setLocalSignupEmbeddedAsButton(true);
		registrationFormLayouts.setPrimaryLayout(formLayout);
		registrationFormLayouts.setSecondaryLayout(formLayout2);

		TranslationAction a1 = new TranslationAction("autoProcess", new String[]
		{ "accept" });
		List<TranslationRule> rules = List.of(new TranslationRule("true", a1));

		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);

		return new RegistrationFormBuilder().withName("f1")
				.withDescription("desc")
				.withDefaultCredentialRequirement("sys:all")
				.withPubliclyAvailable(true)
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
				.withRegistrationCode("123")
				.withCollectComments(true)
				.withDisplayedName(new I18nString("displayedName"))
				.withPageTitle(new I18nString("pageTitle"))
				.withFormLayoutSettings(new FormLayoutSettings(true, 50, "em", true, "logoUrl"))
				.withWrapUpConfig(List.of(new RegistrationWrapUpConfig(TriggeringState.AUTO_ACCEPTED)))
				.withPolicyAgreements(List.of(new PolicyAgreementConfiguration(List.of(1L),
						PolicyAgreementPresentationType.CHECKBOX_SELECTED, new I18nString("text"))))
				.withByInvitationOnly(true)
				.withCheckIdentityOnSubmit(true)
				.withPubliclyAvailable(true)
				.withCaptchaLength(10)
				.withNotificationsConfiguration()
				.withAcceptedTemplate("accept")
				.endNotificationsConfiguration()
				.withTitle2ndStage(new I18nString("secStage"))
				.withFormInformation2ndStage(new I18nString("secStageInfo"))
				.withExternalSignupSpec(
						new ExternalSignupSpec(List.of(new AuthenticationOptionsSelector("key", "option"))))
				.withExternalGridSignupSpec(new ExternalSignupGridSpec(
						List.of(new AuthenticationOptionsSelector("key", "option")), new AuthnGridSettings(true, 10)))

				.withLayouts(registrationFormLayouts)

				.build();

	}

	@Override
	protected DBRegistrationForm getFullDBObject()
	{
		return DBRegistrationForm.builder()
				.withName("f1")
				.withDescription("desc")
				.withDefaultCredentialRequirement("sys:all")
				.withPubliclyAvailable(true)
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
				.withRegistrationCode("123")
				.withCollectComments(true)
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("displayedName")
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
				.withPubliclyAvailable(true)
				.withCaptchaLength(10)
				.withNotificationsConfiguration(DBRegistrationFormNotifications.builder()
						.withAcceptedTemplate("accept")
						.build())
				.withTitle2ndStage(DBI18nString.builder()
						.withDefaultValue("secStage")
						.build())
				.withFormInformation2ndStage(DBI18nString.builder()
						.withDefaultValue("secStageInfo")
						.build())
				.withExternalSignupSpec(DBExternalSignupSpec.builder()
						.withSpecs(List.of(DBAuthenticationOptionsSelector.builder()
								.withAuthenticatorKey("key")
								.withOptionKey("option")
								.build()))
						.build())
				.withExternalSignupGridSpec(DBExternalSignupGridSpec.builder()
						.withGridSettings(DBAuthnGridSettings.builder()
								.withHeight(10)
								.withSearchable(true)
								.build())
						.withSpecs(List.of(DBAuthenticationOptionsSelector.builder()
								.withAuthenticatorKey("key")
								.withOptionKey("option")
								.build()))
						.build())
				.withFormLayouts(DBRegistrationFormLayouts.builder()
						.withLocalSignupEmbeddedAsButton(true)
						.withPrimaryLayout(DBFormLayout.builder()
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
						.withSecondaryLayout(DBFormLayout.builder()
								.withElements(List.of(DBBasicFormElement.builder()
										.withType("ATTRIBUTE")
										.build(),
										DBFormCaptionElement.builder()
												.withValue(DBI18nString.builder()
														.withDefaultValue("value2")
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
						.build())
				.build();
	}

	@Override
	protected RegistrationForm getMinAPIObject()
	{

		return new RegistrationFormBuilder().withName("f1")
				.withDefaultCredentialRequirement("sys:all")
				.build();
	}

	@Override
	protected DBRegistrationForm getMinDBObject()
	{
		return DBRegistrationForm.builder()
				.withName("f1")
				.withDefaultCredentialRequirement("sys:all")
				.build();
	}

	@Override
	protected Pair<Function<RegistrationForm, DBRegistrationForm>, Function<DBRegistrationForm, RegistrationForm>> getMapper()
	{
		return Pair.of(RegistrationFormMapper::map, RegistrationFormMapper::map);
	}

	@Test
	public void shouldSupportStringFormInformation()
	{
		DBRegistrationForm restRegistrationForm = DBRegistrationForm.builder()
				.withName("f1")
				.withFormInformation("formInfo")
				.withDefaultCredentialRequirement("sys:all")
				.build();
		RegistrationForm map = RegistrationFormMapper.map(restRegistrationForm);
		assertThat(map.getFormInformation()
				.getDefaultValue()).isEqualTo("formInfo");
	}

}
