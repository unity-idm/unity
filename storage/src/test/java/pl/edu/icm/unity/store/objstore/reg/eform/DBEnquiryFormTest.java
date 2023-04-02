/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eform;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;
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
import pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile.DBTranslationProfileRule;
import pl.edu.icm.unity.store.types.DBI18nString;

public class DBEnquiryFormTest extends DBTypeTestBase<DBEnquiryForm>
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
	protected DBEnquiryForm getObject()
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
								.withCondition(pl.edu.icm.unity.store.objstore.tprofile.DBTranslationProfile.Condition
										.builder()
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

}
