/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.Optional;

import io.imunity.rest.api.types.registration.RestAgreementRegistrationParam;
import io.imunity.rest.mappers.I18nStringMapper;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;

public class AgreementRegistrationParamMapper
{

	public static RestAgreementRegistrationParam map(AgreementRegistrationParam agreementRegistrationParam)
	{
		return RestAgreementRegistrationParam.builder()
				.withMandatory(agreementRegistrationParam.isManatory())
				.withText(Optional.ofNullable(agreementRegistrationParam.getText())
						.map(I18nStringMapper::map)
						.orElse(null))
				.build();
	}

	public static AgreementRegistrationParam map(RestAgreementRegistrationParam registrationParam)
	{
		return new AgreementRegistrationParam(Optional.ofNullable(registrationParam.text)
				.map(I18nStringMapper::map)
				.orElse(null), registrationParam.mandatory);
	}

}
