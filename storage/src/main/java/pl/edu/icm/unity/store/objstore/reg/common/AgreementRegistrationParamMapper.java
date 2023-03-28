/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Optional;

import pl.edu.icm.unity.store.types.I18nStringMapper;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;

public class AgreementRegistrationParamMapper
{

	public static DBAgreementRegistrationParam map(AgreementRegistrationParam agreementRegistrationParam)
	{
		return DBAgreementRegistrationParam.builder()
				.withMandatory(agreementRegistrationParam.isManatory())
				.withText(Optional.ofNullable(agreementRegistrationParam.getText())
						.map(I18nStringMapper::map)
						.orElse(null))
				.build();
	}

	public static AgreementRegistrationParam map(DBAgreementRegistrationParam registrationParam)
	{
		return new AgreementRegistrationParam(Optional.ofNullable(registrationParam.text)
				.map(I18nStringMapper::map)
				.orElse(null), registrationParam.mandatory);
	}

}
