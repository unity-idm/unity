/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAgreementRegistrationParamTest extends DBTypeTestBase<DBAgreementRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"i18nText\":{\"DefaultValue\":\"a\",\"Map\":{}},\"manatory\":false}";
	}

	@Override
	protected DBAgreementRegistrationParam getObject()
	{
		return DBAgreementRegistrationParamProvider.getRegistrationParam();
	}
}
