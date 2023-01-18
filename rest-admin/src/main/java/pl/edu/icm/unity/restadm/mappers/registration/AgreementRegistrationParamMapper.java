/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.Optional;

import io.imunity.rest.api.types.registration.RestAgreementRegistrationParam;
import pl.edu.icm.unity.restadm.mappers.I18nStringMapper;
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
