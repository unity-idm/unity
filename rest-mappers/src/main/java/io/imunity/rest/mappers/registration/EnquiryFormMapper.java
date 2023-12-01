/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.mappers.I18nStringMapper;
import io.imunity.rest.mappers.policyAgreement.PolicyAgreementConfigurationMapper;
import io.imunity.rest.mappers.registration.layout.FormLayoutMapper;
import io.imunity.rest.mappers.registration.layout.FormLayoutSettingsMapper;
import io.imunity.rest.mappers.translation.TranslationProfileMapper;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

public class EnquiryFormMapper
{
	public static RestEnquiryForm map(EnquiryForm enquiryForm)
	{
		return RestEnquiryForm.builder()
				.withName(enquiryForm.getName())
				.withDescription(enquiryForm.getDescription())
				.withIdentityParams(Optional.ofNullable(enquiryForm.getIdentityParams())
						.map(p -> p.stream()
								.map(i -> Optional.ofNullable(i)
										.map(IdentityRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withAttributeParams(Optional.ofNullable(enquiryForm.getAttributeParams())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AttributeRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withGroupParams(Optional.ofNullable(enquiryForm.getGroupParams())
						.map(p -> p.stream()
								.map(g -> Optional.ofNullable(g)
										.map(GroupRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withCredentialParams(Optional.ofNullable(enquiryForm.getCredentialParams())
						.map(p -> p.stream()
								.map(c -> Optional.ofNullable(c)
										.map(CredentialRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withAgreements(Optional.ofNullable(enquiryForm.getAgreements())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AgreementRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withCollectComments(enquiryForm.isCollectComments())
				.withDisplayedName(Optional.ofNullable(enquiryForm.getDisplayedName())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withFormInformation(Optional.ofNullable(enquiryForm.getFormInformation())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withPageTitle(Optional.ofNullable(enquiryForm.getPageTitle())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withTranslationProfile(Optional.ofNullable(enquiryForm.getTranslationProfile())
						.map(TranslationProfileMapper::map)
						.orElse(null))

				.withLayoutSettings(Optional.ofNullable(enquiryForm.getLayoutSettings())
						.map(FormLayoutSettingsMapper::map)
						.orElse(null))
				.withWrapUpConfig(Optional.ofNullable(enquiryForm.getWrapUpConfig())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(RegistrationWrapUpConfigMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withPolicyAgreements(Optional.ofNullable(enquiryForm.getPolicyAgreements())
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(PolicyAgreementConfigurationMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withByInvitationOnly(enquiryForm.isByInvitationOnly())
				.withCheckIdentityOnSubmit(enquiryForm.isCheckIdentityOnSubmit())
				.withType(enquiryForm.getType()
						.name())
				.withTargetCondition(enquiryForm.getTargetCondition())
				.withTargetGroups(List.of(enquiryForm.getTargetGroups()))
				.withNotificationsConfiguration(Optional.ofNullable(enquiryForm.getNotificationsConfiguration())
						.map(EnquiryFormNotificationsMapper::map)
						.orElse(null))
				.withLayout(Optional.ofNullable(enquiryForm.getLayout())
						.map(FormLayoutMapper::map)
						.orElse(null))
				.build();

	}

	public static EnquiryForm map(RestEnquiryForm restEnquiryForm)
	{
		return new EnquiryFormBuilder().withName(restEnquiryForm.name)
				.withDescription(restEnquiryForm.description)
				.withIdentityParams(Optional.ofNullable(restEnquiryForm.identityParams)
						.map(p -> p.stream()
								.map(i -> Optional.ofNullable(i)
										.map(IdentityRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withAttributeParams(Optional.ofNullable(restEnquiryForm.attributeParams)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AttributeRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withGroupParams(Optional.ofNullable(restEnquiryForm.groupParams)
						.map(p -> p.stream()
								.map(g -> Optional.ofNullable(g)
										.map(GroupRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withCredentialParams(Optional.ofNullable(restEnquiryForm.credentialParams)
						.map(p -> p.stream()
								.map(c -> Optional.ofNullable(c)
										.map(CredentialRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withAgreements(Optional.ofNullable(restEnquiryForm.agreements)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AgreementRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withCollectComments(restEnquiryForm.collectComments)
				.withDisplayedName(Optional.ofNullable(restEnquiryForm.displayedName)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(restEnquiryForm.name)))
				.withFormInformation(Optional.ofNullable(restEnquiryForm.i18nFormInformation)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(restEnquiryForm.formInformation)))
				.withPageTitle(Optional.ofNullable(restEnquiryForm.pageTitle)
						.map(I18nStringMapper::map)
						.orElse(new I18nString()))
				.withTranslationProfile(Optional.ofNullable(restEnquiryForm.translationProfile)
						.map(TranslationProfileMapper::map)
						.orElse(new TranslationProfile("registrationProfile", "", ProfileType.REGISTRATION, new ArrayList<>())))
				.withWrapUpConfig(Optional.ofNullable(restEnquiryForm.wrapUpConfig)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(RegistrationWrapUpConfigMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withPolicyAgreements(Optional.ofNullable(restEnquiryForm.policyAgreements)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(PolicyAgreementConfigurationMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withFormLayoutSettings(Optional.ofNullable(restEnquiryForm.layoutSettings)
						.map(FormLayoutSettingsMapper::map)
						.orElse(FormLayoutSettings.DEFAULT))
				.withByInvitationOnly(restEnquiryForm.byInvitationOnly)
				.withCheckIdentityOnSubmit(restEnquiryForm.checkIdentityOnSubmit)
				.withType(EnquiryType.valueOf(restEnquiryForm.type))
				.withTargetGroups(restEnquiryForm.targetGroups.toArray(String[]::new))
				.withTargetCondition(restEnquiryForm.targetCondition)
				.withNotificationsConfiguration(Optional.ofNullable(restEnquiryForm.notificationsConfiguration)
						.map(EnquiryFormNotificationsMapper::map)
						.orElse(new EnquiryFormNotifications()))
				.withLayout(Optional.ofNullable(restEnquiryForm.layout)
						.map(FormLayoutMapper::map)
						.orElse(null))
				.build();

	}

}
