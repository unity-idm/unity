/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.rest.mappers.I18nStringMapper;
import io.imunity.rest.mappers.policyAgreement.PolicyAgreementConfigurationMapper;
import io.imunity.rest.mappers.registration.layout.FormLayoutSettingsMapper;
import io.imunity.rest.mappers.translation.TranslationProfileMapper;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.base.registration.ExternalSignupSpec;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.base.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.base.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;

public class RegistrationFormMapper
{
	public static RestRegistrationForm map(RegistrationForm registrationForm)
	{
		return RestRegistrationForm.builder()
				.withName(registrationForm.getName())
				.withDescription(registrationForm.getDescription())
				.withIdentityParams(Optional.ofNullable(registrationForm.getIdentityParams())
						.map(p -> p.stream()
								.map(i -> Optional.ofNullable(i)
										.map(IdentityRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withAttributeParams(Optional.ofNullable(registrationForm.getAttributeParams())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AttributeRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withGroupParams(Optional.ofNullable(registrationForm.getGroupParams())
						.map(p -> p.stream()
								.map(g -> Optional.ofNullable(g)
										.map(GroupRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withCredentialParams(Optional.ofNullable(registrationForm.getCredentialParams())
						.map(p -> p.stream()
								.map(c -> Optional.ofNullable(c)
										.map(CredentialRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withAgreements(Optional.ofNullable(registrationForm.getAgreements())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AgreementRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withCollectComments(registrationForm.isCollectComments())
				.withDisplayedName(Optional.ofNullable(registrationForm.getDisplayedName())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withFormInformation(Optional.ofNullable(registrationForm.getFormInformation())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withPageTitle(Optional.ofNullable(registrationForm.getPageTitle())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withTranslationProfile(Optional.ofNullable(registrationForm.getTranslationProfile())
						.map(TranslationProfileMapper::map)
						.orElse(null))
				.withFormLayouts(Optional.ofNullable(registrationForm.getFormLayouts())
						.map(RegistrationFormLayoutsMapper::map)
						.orElse(null))
				.withLayoutSettings(Optional.ofNullable(registrationForm.getLayoutSettings())
						.map(FormLayoutSettingsMapper::map)
						.orElse(null))
				.withWrapUpConfig(Optional.ofNullable(registrationForm.getWrapUpConfig())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(RegistrationWrapUpConfigMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withPolicyAgreements(Optional.ofNullable(registrationForm.getPolicyAgreements())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(PolicyAgreementConfigurationMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withByInvitationOnly(registrationForm.isByInvitationOnly())
				.withCheckIdentityOnSubmit(registrationForm.isCheckIdentityOnSubmit())
				.withPubliclyAvailable(registrationForm.isPubliclyAvailable())
				.withNotificationsConfiguration(Optional.ofNullable(registrationForm.getNotificationsConfiguration())
						.map(RegistrationFormNotificationsMapper::map)
						.orElse(null))
				.withCaptchaLength(registrationForm.getCaptchaLength())
				.withRegistrationCode(registrationForm.getRegistrationCode())
				.withDefaultCredentialRequirement(registrationForm.getDefaultCredentialRequirement())
				.withTitle2ndStage(Optional.ofNullable(registrationForm.getTitle2ndStage())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withFormInformation2ndStage(Optional.ofNullable(registrationForm.getFormInformation2ndStage())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withExternalSignupGridSpec(Optional.ofNullable(registrationForm.getExternalSignupGridSpec())
						.map(ExternalSignupGridSpecMapper::map)
						.orElse(null))
				.withExternalSignupSpec(Optional.ofNullable(registrationForm.getExternalSignupSpec())
						.map(ExternalSignupSpecMapper::map)
						.orElse(null))
				.withShowSignInLink(registrationForm.isShowSignInLink())
				.withSignInLink(registrationForm.getSignInLink())
				.withSwitchToEnquiryInfo(Optional.ofNullable(registrationForm.getSwitchToEnquiryInfo())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withAutoLoginToRealm(registrationForm.getAutoLoginToRealm())
				.build();

	}

	public static RegistrationForm map(RestRegistrationForm restRegistrationForm)
	{
		return new RegistrationFormBuilder().withName(restRegistrationForm.name)
				.withDescription(restRegistrationForm.description)
				.withIdentityParams(Optional.ofNullable(restRegistrationForm.identityParams)
						.map(p -> p.stream()
								.map(i -> Optional.ofNullable(i)
										.map(IdentityRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withAttributeParams(Optional.ofNullable(restRegistrationForm.attributeParams)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AttributeRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withGroupParams(Optional.ofNullable(restRegistrationForm.groupParams)
						.map(p -> p.stream()
								.map(g -> Optional.ofNullable(g)
										.map(GroupRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withCredentialParams(Optional.ofNullable(restRegistrationForm.credentialParams)
						.map(p -> p.stream()
								.map(c -> Optional.ofNullable(c)
										.map(CredentialRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withAgreements(Optional.ofNullable(restRegistrationForm.agreements)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AgreementRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withCollectComments(restRegistrationForm.collectComments)
				.withDisplayedName(Optional.ofNullable(restRegistrationForm.displayedName)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(restRegistrationForm.name)))
				.withFormInformation(Optional.ofNullable(restRegistrationForm.i18nFormInformation)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(restRegistrationForm.formInformation)))
				.withPageTitle(Optional.ofNullable(restRegistrationForm.pageTitle)
						.map(I18nStringMapper::map)
						.orElse(new I18nString()))
				.withTranslationProfile(Optional.ofNullable(restRegistrationForm.translationProfile)
						.map(TranslationProfileMapper::map)
						.orElse(new TranslationProfile("registrationProfile", "", ProfileType.REGISTRATION, new ArrayList<>())))
				.withLayouts(Optional.ofNullable(restRegistrationForm.formLayouts)
						.map(RegistrationFormLayoutsMapper::map)
						.orElse(new RegistrationFormLayouts()))
				.withFormLayoutSettings(Optional.ofNullable(restRegistrationForm.layoutSettings)
						.map(FormLayoutSettingsMapper::map)
						.orElse(FormLayoutSettings.DEFAULT))
				.withWrapUpConfig(Optional.ofNullable(restRegistrationForm.wrapUpConfig)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(RegistrationWrapUpConfigMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withPolicyAgreements(Optional.ofNullable(restRegistrationForm.policyAgreements)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(PolicyAgreementConfigurationMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withByInvitationOnly(restRegistrationForm.byInvitationOnly)
				.withCheckIdentityOnSubmit(restRegistrationForm.checkIdentityOnSubmit)
				.withPubliclyAvailable(restRegistrationForm.publiclyAvailable)
				.withNotificationsConfiguration(Optional.ofNullable(restRegistrationForm.notificationsConfiguration)
						.map(RegistrationFormNotificationsMapper::map)
						.orElse(new RegistrationFormNotifications()))
				.withCaptchaLength(restRegistrationForm.captchaLength)
				.withRegistrationCode(restRegistrationForm.registrationCode)
				.withDefaultCredentialRequirement(restRegistrationForm.defaultCredentialRequirement)
				.withTitle2ndStage(Optional.ofNullable(restRegistrationForm.title2ndStage)
						.map(I18nStringMapper::map)
						.orElse(new I18nString()))
				.withFormInformation2ndStage(Optional.ofNullable(restRegistrationForm.formInformation2ndStage)
						.map(I18nStringMapper::map)
						.orElse(new I18nString()))
				.withExternalGridSignupSpec(Optional.ofNullable(restRegistrationForm.externalSignupGridSpec)
						.map(ExternalSignupGridSpecMapper::map)
						.orElse(new ExternalSignupGridSpec()))
				.withExternalSignupSpec(Optional.ofNullable(restRegistrationForm.externalSignupSpec)
						.map(ExternalSignupSpecMapper::map)
						.orElse(new ExternalSignupSpec()))
				.withShowGotoSignIn(restRegistrationForm.showSignInLink, restRegistrationForm.signInLink)
				.withSwitchToEnquiryInfo(Optional.ofNullable(restRegistrationForm.switchToEnquiryInfo)
						.map(I18nStringMapper::map)
						.orElse(null))
				.withAutoLoginToRealm(restRegistrationForm.autoLoginToRealm)
				.build();

	}

}
