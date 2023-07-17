/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementConfiguration;
import io.imunity.rest.api.types.registration.RestAgreementRegistrationParam;
import io.imunity.rest.api.types.registration.RestAttributeRegistrationParam;
import io.imunity.rest.api.types.registration.RestCredentialRegistrationParam;
import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestEnquiryFormNotifications;
import io.imunity.rest.api.types.registration.RestGroupRegistrationParam;
import io.imunity.rest.api.types.registration.RestIdentityRegistrationParam;
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
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.base.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
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
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.restadm.mappers.MapperWithMinimalTestBase;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class RestEnquiryFormMapperTest extends MapperWithMinimalTestBase<EnquiryForm, RestEnquiryForm>
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

		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, new String[]
		{ AutomaticRequestAction.accept.toString() });
		List<TranslationRule> rules = List.of(new TranslationRule("true", a1));

		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);

		return new EnquiryFormBuilder().withName("f1")
				.withDescription("desc")
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
	protected RestEnquiryForm getFullRestObject()
	{
		return RestEnquiryForm.builder()
				.withName("f1")
				.withDescription("desc")
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
				.withLayout(RestFormLayout.builder()
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
				.withType("REQUESTED_MANDATORY")
				.withTargetGroups(List.of("/"))
				.withTargetCondition("true")
				.withNotificationsConfiguration(RestEnquiryFormNotifications.builder()
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
	protected RestEnquiryForm getMinRestObject()
	{
		return RestEnquiryForm.builder()
				.withName("f1")
				.withType("REQUESTED_MANDATORY")
				.withTargetGroups(List.of("/"))
				.build();
	}
	
	@Test
	public void shouldSupportStringFormInformation()
	{
		RestEnquiryForm restEnquiryForm = RestEnquiryForm.builder()
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
	protected Pair<Function<EnquiryForm, RestEnquiryForm>, Function<RestEnquiryForm, EnquiryForm>> getMapper()
	{
		return Pair.of(EnquiryFormMapper::map, EnquiryFormMapper::map);
	}

}
