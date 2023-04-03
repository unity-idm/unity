/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;

public class AgreementRegistrationParamMapperTest
		extends MapperTestBase<AgreementRegistrationParam, DBAgreementRegistrationParam>
{

	@Override
	protected AgreementRegistrationParam getFullAPIObject()
	{
		return new AgreementRegistrationParam(new I18nString("testV"), true);
	}

	@Override
	protected DBAgreementRegistrationParam getFullDBObject()
	{
		return DBAgreementRegistrationParam.builder()
				.withMandatory(true)
				.withText(DBI18nString.builder()
						.withDefaultValue("testV")
						.build())
				.build();
	}

	@Override
	protected Pair<Function<AgreementRegistrationParam, DBAgreementRegistrationParam>, Function<DBAgreementRegistrationParam, AgreementRegistrationParam>> getMapper()
	{
		return Pair.of(AgreementRegistrationParamMapper::map, AgreementRegistrationParamMapper::map);
	}

}
