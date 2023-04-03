/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.Optional;

import pl.edu.icm.unity.store.objstore.reg.common.BaseFormMapper;
import pl.edu.icm.unity.store.types.common.I18nStringMapper;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.types.registration.ExternalSignupSpec;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;

class RegistrationFormMapper
{
	static DBRegistrationForm map(RegistrationForm registrationForm)
	{
		return BaseFormMapper.map(DBRegistrationForm.builder(), registrationForm)
				.withFormLayouts(Optional.ofNullable(registrationForm.getFormLayouts())
						.map(RegistrationFormLayoutsMapper::map)
						.orElse(null))
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

	static RegistrationForm map(DBRegistrationForm restRegistrationForm)
	{
		return BaseFormMapper.map(new RegistrationFormBuilder(), restRegistrationForm)
				.withLayouts(Optional.ofNullable(restRegistrationForm.formLayouts)
						.map(RegistrationFormLayoutsMapper::map)
						.orElse(new RegistrationFormLayouts()))
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
