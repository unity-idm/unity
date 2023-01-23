/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.registration.RestAgreementRegistrationParam;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;

public class AgreementRegistrationParamMapperTest
		extends MapperTestBase<AgreementRegistrationParam, RestAgreementRegistrationParam>
{

	@Override
	protected AgreementRegistrationParam getFullAPIObject()
	{
		return new AgreementRegistrationParam(new I18nString("testV"), true);
	}

	@Override
	protected RestAgreementRegistrationParam getFullRestObject()
	{
		return RestAgreementRegistrationParam.builder()
				.withMandatory(true)
				.withText(RestI18nString.builder()
						.withDefaultValue("testV")
						.build())
				.build();
	}

	@Override
	protected Pair<Function<AgreementRegistrationParam, RestAgreementRegistrationParam>, Function<RestAgreementRegistrationParam, AgreementRegistrationParam>> getMapper()
	{
		return Pair.of(AgreementRegistrationParamMapper::map, AgreementRegistrationParamMapper::map);
	}

}
