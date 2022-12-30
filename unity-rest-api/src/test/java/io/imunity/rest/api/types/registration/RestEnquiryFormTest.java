/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.List;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.basic.RestTypeBase;
import io.imunity.rest.api.types.policyAgreement.RestPolicyAgreementConfiguration;
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

public class RestEnquiryFormTest extends RestTypeBase<RestEnquiryForm>
{

	@Override
	protected String getJson()
	{
		return "{\"Agreements\":[{\"i18nText\":{\"DefaultValue\":\"a\",\"Map\":{}},\"manatory\":false}],"
				+ "\"AttributeParams\":[{\"label\":null,\"description\":null,\"retrievalSettings\":\"interactive\","
				+ "\"optional\":true,\"attributeType\":\"email\",\"group\":\"/\",\"showGroups\":true,\"useDescription\":false,"
				+ "\"confirmationMode\":\"ON_SUBMIT\",\"urlQueryPrefill\":null}],\"CollectComments\":true,"
				+ "\"CredentialParams\":[{\"credentialName\":\"sys:password\",\"label\":null,\"description\":null}],\"Description\":\"desc\","
				+ "\"i18nFormInformation\":{\"DefaultValue\":\"formInfo\",\"Map\":{}},\"GroupParams\":[{\"label\":null,\"description\":null,"
				+ "\"retrievalSettings\":\"automatic\",\"groupPath\":\"/B\",\"multiSelect\":false,\"includeGroupsMode\":\"all\"}],"
				+ "\"IdentityParams\":[{\"label\":null,\"description\":null,\"retrievalSettings\":\"automaticHidden\",\"optional\":false,"
				+ "\"identityType\":\"userName\",\"confirmationMode\":\"ON_SUBMIT\",\"urlQueryPrefill\":null}],\"Name\":\"f1\","
				+ "\"DisplayedName\":{\"DefaultValue\":\"displayedName\",\"Map\":{}},\"TranslationProfile\":{\"ver\":\"2\",\"name\":\"form\","
				+ "\"description\":\"\",\"type\":\"REGISTRATION\",\"mode\":\"DEFAULT\",\"rules\":[{\"condition\":{\"conditionValue\":\"true\"},"
				+ "\"action\":{\"name\":\"autoProcess\",\"parameters\":[\"accept\"]}}]},\"FormLayoutSettings\":{\"compactInputs\":true,"
				+ "\"showCancel\":true,\"columnWidth\":50.0,\"columnWidthUnit\":\"em\",\"logoURL\":\"logoUrl\"},"
				+ "\"PageTitle\":{\"DefaultValue\":\"pageTitle\",\"Map\":{}},\"WrapUpConfig\":[{\"state\":\"AUTO_ACCEPTED\","
				+ "\"title\":null,\"info\":null,\"redirectCaption\":null,\"automatic\":false,\"redirectURL\":null,\"redirectAfterTime\":null}],"
				+ "\"ByInvitationOnly\":true,\"PolicyAgreements\":[{\"documentsIdsToAccept\":[1],\"presentationType\":\"CHECKBOX_SELECTED\","
				+ "\"text\":{\"DefaultValue\":\"text\",\"Map\":{}}}],\"CheckIdentityOnSubmit\":true,\"type\":\"REQUESTED_MANDATORY\","
				+ "\"targetGroups\":[\"/\"],\"NotificationsConfiguration\":{\"rejectedTemplate\":\"rejectTemplate\","
				+ "\"acceptedTemplate\":\"acceptedTemplate\",\"updatedTemplate\":\"updatedTemplate\",\"invitationTemplate\":\"invitationTemplate\","
				+ "\"invitationProcessedTemplate\":\"invitationProcessedTemplate\",\"adminsNotificationGroup\":\"/group\","
				+ "\"sendUserNotificationCopyToAdmin\":true,\"enquiryToFillTemplate\":\"enquiryToFillTemplate\","
				+ "\"submittedTemplate\":\"submittedTemplate\"},\"FormLayout\":{\"elements\":[{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.BasicFormElement\","
				+ "\"type\":\"REG_CODE\",\"formContentsRelated\":true},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormCaptionElement\","
				+ "\"type\":\"CAPTION\",\"formContentsRelated\":false,\"value\":{\"DefaultValue\":\"value\","
				+ "\"Map\":{}}},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormParameterElement\",\"type\":\"ATTRIBUTE\","
				+ "\"formContentsRelated\":true,\"index\":1},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormSeparatorElement\","
				+ "\"type\":\"SEPARATOR\",\"formContentsRelated\":false},{\"clazz\":\"pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement\","
				+ "\"type\":\"LOCAL_SIGNUP\",\"formContentsRelated\":true}]},\"targetCondition\":\"true\"}\n";
	}

	@Override
	protected RestEnquiryForm getObject()
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

}
