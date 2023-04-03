/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.imunity.rest.api.types.registration.RestAuthnGridSettings;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.GroupProperty;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementDecision;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.ConfirmationMode;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec.AuthnGridSettings;
import pl.edu.icm.unity.types.registration.ExternalSignupSpec;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam.IncludeGroupsMode;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
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

public class MapperRegTest2
{
	@Test
	public void test() throws JsonProcessingException
	{

		URLQueryPrefillConfig urlQueryPrefillConfig = new URLQueryPrefillConfig("param", PrefilledEntryMode.DEFAULT);

		// System.out.println(Constants.MAPPER.writeValueAsString(urlQueryPrefillConfig));

		IdentityRegistrationParam identityRegistrationParam = new IdentityRegistrationParam();
		identityRegistrationParam.setConfirmationMode(ConfirmationMode.CONFIRMED);
		identityRegistrationParam.setDescription("desc");
		identityRegistrationParam.setIdentityType("type");
		identityRegistrationParam.setLabel("label");
		identityRegistrationParam.setOptional(false);
		identityRegistrationParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		identityRegistrationParam.setUrlQueryPrefill(urlQueryPrefillConfig);

		AttributeRegistrationParam attributeRegistrationParam = new AttributeRegistrationParam();

		attributeRegistrationParam.setConfirmationMode(ConfirmationMode.CONFIRMED);
		attributeRegistrationParam.setDescription("desc");
		attributeRegistrationParam.setAttributeType("type");
		attributeRegistrationParam.setLabel("label");
		attributeRegistrationParam.setOptional(false);
		attributeRegistrationParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		attributeRegistrationParam.setUrlQueryPrefill(urlQueryPrefillConfig);
		attributeRegistrationParam.setShowGroups(true);
		attributeRegistrationParam.setUseDescription(true);
		attributeRegistrationParam.setGroup("/");

		CredentialRegistrationParam credentialRegistrationParam = new CredentialRegistrationParam("name", "label",
				"desc");

		AgreementRegistrationParam agreementRegistrationParam = new AgreementRegistrationParam(new I18nString("testV"),
				true);

		GroupRegistrationParam groupRegistrationParam = new GroupRegistrationParam();
		groupRegistrationParam.setDescription("desc");
		groupRegistrationParam.setGroupPath("/group");
		groupRegistrationParam.setIncludeGroupsMode(IncludeGroupsMode.all);
		groupRegistrationParam.setMultiSelect(true);
		groupRegistrationParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		groupRegistrationParam.setLabel("label");

		FormLayoutSettings formLayoutSettings = new FormLayoutSettings(true, 10, "EM", false, "logo");

		RegistrationWrapUpConfig registrationWrapUpConfig = new RegistrationWrapUpConfig(TriggeringState.AUTO_ACCEPTED,
				new I18nString("title"), new I18nString("info"), new I18nString("redirect"), true, "redirectUrl",
				Duration.ofDays(1));

		PolicyAgreementConfiguration policyAgreementConfiguration = new PolicyAgreementConfiguration(List.of(1l, 2l),
				PolicyAgreementPresentationType.CHECKBOX_SELECTED, new I18nString("text"));

		FormLayout formLayout = new FormLayout(List.of(new BasicFormElement(FormLayoutElement.REG_CODE),
				new FormCaptionElement(new I18nString("value")),
				new FormParameterElement(FormLayoutElement.ATTRIBUTE, 1), new FormSeparatorElement(),
				new FormLocalSignupButtonElement()));

		FormLayout formLayout2 = new FormLayout(List.of(new BasicFormElement(FormLayoutElement.ATTRIBUTE),
				new FormCaptionElement(new I18nString("value2")),
				new FormParameterElement(FormLayoutElement.ATTRIBUTE, 1), new FormSeparatorElement(),
				new FormLocalSignupButtonElement()));

		RegistrationFormNotifications registrationFormNotifications = new RegistrationFormNotifications();
		registrationFormNotifications.setAcceptedTemplate("acceptedTemplate");
		registrationFormNotifications.setAdminsNotificationGroup("/group");
		registrationFormNotifications.setInvitationProcessedTemplate("invitationProcessedTemplate");
		registrationFormNotifications.setInvitationTemplate("invitationTemplate");
		registrationFormNotifications.setRejectedTemplate("rejectTemplate");
		registrationFormNotifications.setSendUserNotificationCopyToAdmin(true);
		registrationFormNotifications.setSubmittedTemplate("submittedTemplate");
		registrationFormNotifications.setUpdatedTemplate("updatedTemplate");

		ExternalSignupSpec externalSignupSpec = new ExternalSignupSpec(
				List.of(new AuthenticationOptionsSelector("key", "option")));

		ExternalSignupGridSpec externalSignupGridSpec = new ExternalSignupGridSpec(
				List.of(new AuthenticationOptionsSelector("key", "option")), new AuthnGridSettings(true, 1));

		RegistrationFormLayouts registrationFormLayouts = new RegistrationFormLayouts();
		registrationFormLayouts.setLocalSignupEmbeddedAsButton(true);
		registrationFormLayouts.setPrimaryLayout(formLayout);
		registrationFormLayouts.setSecondaryLayout(formLayout2);

		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, new String[]
		{ AutomaticRequestAction.accept.toString() });
		List<TranslationRule> rules = List.of(new TranslationRule("true", a1));

		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);

		RegistrationForm registrationForm = new RegistrationFormBuilder().withName("f1")
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
		
		
		EnquiryForm enquiryForm = new EnquiryFormBuilder().withName("f1")
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
				.withTargetGroups(new String[] {"/"})
				.withTargetCondition("true")
				.withNotificationsConfiguration(enquiryFormNotifications)
				.build();
		
		
		
		
		
		System.out.println(Constants.MAPPER.writeValueAsString(enquiryForm));

	}
}
