/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.BaseFormBuilder;
import pl.edu.icm.unity.base.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.store.objstore.reg.common.DBBaseForm.DBBaseFormBuilder;
import pl.edu.icm.unity.store.objstore.reg.layout.FormLayoutSettingsMapper;
import pl.edu.icm.unity.store.objstore.tprofile.TranslationProfileMapper;
import pl.edu.icm.unity.store.types.common.I18nStringMapper;

public class BaseFormMapper
{
	@SuppressWarnings("unchecked")
	public static <T extends DBBaseFormBuilder<?>> T map(DBBaseFormBuilder<T> builder, BaseForm enquiryForm)
	{
		return (T) builder.withName(enquiryForm.getName())
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
				.withCheckIdentityOnSubmit(enquiryForm.isCheckIdentityOnSubmit());
	}

	@SuppressWarnings("unchecked")
	public static <T extends BaseFormBuilder<?>> T map(BaseFormBuilder<T> builder, DBBaseForm dbEnquiryForm)
	{
		return (T) builder.withName(dbEnquiryForm.name)
				.withDescription(dbEnquiryForm.description)
				.withIdentityParams(Optional.ofNullable(dbEnquiryForm.identityParams)
						.map(p -> p.stream()
								.map(i -> Optional.ofNullable(i)
										.map(IdentityRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withAttributeParams(Optional.ofNullable(dbEnquiryForm.attributeParams)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AttributeRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withGroupParams(Optional.ofNullable(dbEnquiryForm.groupParams)
						.map(p -> p.stream()
								.map(g -> Optional.ofNullable(g)
										.map(GroupRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withCredentialParams(Optional.ofNullable(dbEnquiryForm.credentialParams)
						.map(p -> p.stream()
								.map(c -> Optional.ofNullable(c)
										.map(CredentialRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withAgreements(Optional.ofNullable(dbEnquiryForm.agreements)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(AgreementRegistrationParamMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withCollectComments(dbEnquiryForm.collectComments)
				.withDisplayedName(Optional.ofNullable(dbEnquiryForm.displayedName)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(dbEnquiryForm.name)))
				.withFormInformation(Optional.ofNullable(dbEnquiryForm.i18nFormInformation)
						.map(I18nStringMapper::map)
						.orElse(new I18nString(dbEnquiryForm.formInformation)))
				.withPageTitle(Optional.ofNullable(dbEnquiryForm.pageTitle)
						.map(I18nStringMapper::map)
						.orElse(new I18nString()))
				.withTranslationProfile(Optional.ofNullable(dbEnquiryForm.translationProfile)
						.map(TranslationProfileMapper::map)
						.orElse(new TranslationProfile("registrationProfile", "", ProfileType.REGISTRATION,
								new ArrayList<>())))
				.withWrapUpConfig(Optional.ofNullable(dbEnquiryForm.wrapUpConfig)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(RegistrationWrapUpConfigMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withPolicyAgreements(Optional.ofNullable(dbEnquiryForm.policyAgreements)
						.map(p -> p.stream()
								.map(a -> Optional.ofNullable(a)
										.map(PolicyAgreementConfigurationMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(new ArrayList<>()))
				.withFormLayoutSettings(Optional.ofNullable(dbEnquiryForm.layoutSettings)
						.map(FormLayoutSettingsMapper::map)
						.orElse(FormLayoutSettings.DEFAULT))
				.withByInvitationOnly(dbEnquiryForm.byInvitationOnly)
				.withCheckIdentityOnSubmit(dbEnquiryForm.checkIdentityOnSubmit);
	}

}
