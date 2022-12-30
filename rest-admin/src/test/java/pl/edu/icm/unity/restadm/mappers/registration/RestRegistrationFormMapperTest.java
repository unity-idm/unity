/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.authn.RestAuthenticationOptionsSelector;
import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementConfiguration;
import io.imunity.rest.api.types.registration.RestAgreementRegistrationParam;
import io.imunity.rest.api.types.registration.RestAttributeRegistrationParam;
import io.imunity.rest.api.types.registration.RestAuthnGridSettings;
import io.imunity.rest.api.types.registration.RestCredentialRegistrationParam;
import io.imunity.rest.api.types.registration.RestExternalSignupGridSpec;
import io.imunity.rest.api.types.registration.RestExternalSignupSpec;
import io.imunity.rest.api.types.registration.RestGroupRegistrationParam;
import io.imunity.rest.api.types.registration.RestIdentityRegistrationParam;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.rest.api.types.registration.RestRegistrationFormLayouts;
import io.imunity.rest.api.types.registration.RestRegistrationFormNotifications;
import io.imunity.rest.api.types.registration.RestRegistrationWrapUpConfig;
import io.imunity.rest.api.types.registration.layout.RestBasicFormElement;
import io.imunity.rest.api.types.registration.layout.RestFormCaptionElement;
import io.imunity.rest.api.types.registration.layout.RestFormLayout;
import io.imunity.rest.api.types.registration.layout.RestFormLayoutSettings;
import io.imunity.rest.api.types.registration.layout.RestFormLocalSignupButtonElement;
import io.imunity.rest.api.types.registration.layout.RestFormParameterElement;
import io.imunity.rest.api.types.registration.layout.RestFormSeparatorElement;
import io.imunity.rest.api.types.translation.RestTranslationAction;
import io.imunity.rest.api.types.translation.RestTranslationProfile;
import io.imunity.rest.api.types.translation.RestTranslationProfile.Condition;
import io.imunity.rest.api.types.translation.RestTranslationProfile.RestTranslationProfileRule;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.types.registration.ExternalSignupSpec;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec.AuthnGridSettings;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.types.registration.layout.FormSeparatorElement;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class RestRegistrationFormMapperTest extends MapperTestBase<RegistrationForm, RestRegistrationForm>
{

	@Override
	protected RegistrationForm getAPIObject()
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

		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, new String[]
		{ AutomaticRequestAction.accept.toString() });
		List<TranslationRule> rules = List.of(new TranslationRule("true", a1));

		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);

		return new RegistrationFormBuilder().withName("f1")
				.withDescription("desc")
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withTranslationProfile(tp)
				.withCollectComments(true)
				.withFormInformation(new I18nString("formInformation"))
				.withAddedCredentialParam(
						new CredentialRegistrationParam(EngineInitialization.DEFAULT_CREDENTIAL, null, null))
				.withAddedAgreement(new AgreementRegistrationParam(new I18nString("a"), false))
				.withAddedIdentityParam()
				.withIdentityType(UsernameIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.EMAIL_ATTR)
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
				.withFormInformation(new I18nString("formInfo"))
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
				.withExternalSignupSpec(
						new ExternalSignupSpec(List.of(new AuthenticationOptionsSelector("key", "option"))))
				.withExternalGridSignupSpec(new ExternalSignupGridSpec(
						List.of(new AuthenticationOptionsSelector("key", "option")), new AuthnGridSettings(true, 10)))

				.withLayouts(registrationFormLayouts)

				.build();

	}

	@Override
	protected RestRegistrationForm getRestObject()
	{
		return RestRegistrationForm.builder()
				.withName("f1")
				.withDescription("desc")
				.withDefaultCredentialRequirement("sys:all")
				.withPubliclyAvailable(true)
				.withTranslationProfile(RestTranslationProfile.builder()
						.withName("form")
						.withDescription("")
						.withType("REGISTRATION")
						.withMode("DEFAULT")
						.withRules(List.of(RestTranslationProfileRule.builder()
								.withCondition(Condition.builder()
										.withConditionValue("true")
										.build())
								.withAction(RestTranslationAction.builder()
										.withName("autoProcess")
										.withParameters(List.of("accept"))
										.build())
								.build()))
						.build())
				.withCollectComments(true)
				.withFormInformation(RestI18nString.builder()
						.withDefaultValue("formInformation")
						.build())
				.withCredentialParams(List.of(RestCredentialRegistrationParam.builder()
						.withCredentialName("sys:password")
						.build()))
				.withAgreements(List.of(RestAgreementRegistrationParam.builder()
						.withMandatory(false)
						.withText(RestI18nString.builder()
								.withDefaultValue("a")
								.build())
						.build()))
				.withIdentityParams(List.of(RestIdentityRegistrationParam.builder()
						.withIdentityType("userName")
						.withRetrievalSettings("automaticHidden")
						.withConfirmationMode("ON_SUBMIT")
						.build()))
				.withAttributeParams(List.of(RestAttributeRegistrationParam.builder()
						.withAttributeType("email")
						.withGroup("/")
						.withOptional(true)
						.withRetrievalSettings("interactive")
						.withShowGroups(true)
						.withConfirmationMode("ON_SUBMIT")
						.build()))
				.withGroupParams(List.of(RestGroupRegistrationParam.builder()
						.withGroupPath("/B")
						.withRetrievalSettings("automatic")
						.withIncludeGroupsMode("all")
						.withMultiSelect(false)
						.build()))
				.withRegistrationCode("123")
				.withCollectComments(true)
				.withDisplayedName(RestI18nString.builder()
						.withDefaultValue("displayedName")
						.build())
				.withFormInformation(RestI18nString.builder()
						.withDefaultValue("formInfo")
						.build())
				.withPageTitle(RestI18nString.builder()
						.withDefaultValue("pageTitle")
						.build())
				.withLayoutSettings(RestFormLayoutSettings.builder()
						.withCompactInputs(true)
						.withColumnWidth(50)
						.withColumnWidthUnit("em")
						.withLogoURL("logoUrl")
						.withShowCancel(true)
						.build())
				.withWrapUpConfig(List.of(RestRegistrationWrapUpConfig.builder()
						.withState("AUTO_ACCEPTED")
						.build()))
				.withPolicyAgreements(List.of(RestPolicyAgreementConfiguration.builder()
						.withDocumentsIdsToAccept(List.of(1L))
						.withPresentationType("CHECKBOX_SELECTED")
						.withText(RestI18nString.builder()
								.withDefaultValue("text")
								.build())
						.build()))
				.withByInvitationOnly(true)
				.withCheckIdentityOnSubmit(true)
				.withPubliclyAvailable(true)
				.withCaptchaLength(10)
				.withNotificationsConfiguration(RestRegistrationFormNotifications.builder()
						.withAcceptedTemplate("accept")
						.build())
				.withTitle2ndStage(RestI18nString.builder()
						.withDefaultValue("secStage")
						.build())
				.withExternalSignupSpec(RestExternalSignupSpec.builder()
						.withSpecs(List.of(RestAuthenticationOptionsSelector.builder()
								.withAuthenticatorKey("key")
								.withOptionKey("option")
								.build()))
						.build())
				.withExternalSignupGridSpec(RestExternalSignupGridSpec.builder()
						.withGridSettings(RestAuthnGridSettings.builder()
								.withHeight(10)
								.withSearchable(true)
								.build())
						.withSpecs(List.of(RestAuthenticationOptionsSelector.builder()
								.withAuthenticatorKey("key")
								.withOptionKey("option")
								.build()))
						.build())
				.withFormLayouts(RestRegistrationFormLayouts.builder()
						.withLocalSignupEmbeddedAsButton(true)
						.withPrimaryLayout(RestFormLayout.builder()
								.withElements(List.of(RestBasicFormElement.builder()
										.withType("REG_CODE")
										.build(),
										RestFormCaptionElement.builder()
												.withValue(RestI18nString.builder()
														.withDefaultValue("value")
														.build())
												.build(),
										RestFormParameterElement.builder()
												.withIndex(1)
												.withType("ATTRIBUTE")
												.build(),
										RestFormSeparatorElement.builder()
												.build(),
										RestFormLocalSignupButtonElement.builder()
												.build()))
								.build())
						.withSecondaryLayout(RestFormLayout.builder()
								.withElements(List.of(RestBasicFormElement.builder()
										.withType("ATTRIBUTE")
										.build(),
										RestFormCaptionElement.builder()
												.withValue(RestI18nString.builder()
														.withDefaultValue("value2")
														.build())
												.build(),
										RestFormParameterElement.builder()
												.withIndex(1)
												.withType("ATTRIBUTE")
												.build(),
										RestFormSeparatorElement.builder()
												.build(),
										RestFormLocalSignupButtonElement.builder()
												.build()))
								.build())
						.build())
				.build();
	}

	@Override
	protected Pair<Function<RegistrationForm, RestRegistrationForm>, Function<RestRegistrationForm, RegistrationForm>> getMapper()
	{
		return Pair.of(RegistrationFormMapper::map, RegistrationFormMapper::map);
	}

}
