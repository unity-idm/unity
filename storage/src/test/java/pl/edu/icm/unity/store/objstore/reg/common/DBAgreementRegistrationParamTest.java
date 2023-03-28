/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.DBI18nString;

public class DBAgreementRegistrationParamTest extends DBTypeTestBase<DBAgreementRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"i18nText\":{\"DefaultValue\":\"testV\",\"Map\":{}},\"manatory\":true}";
	}

	@Override
	protected DBAgreementRegistrationParam getObject()
	{
		return DBAgreementRegistrationParam.builder()
				.withMandatory(true)
				.withText(DBI18nString.builder()
						.withDefaultValue("testV")
						.build())
				.build();
	}

}
