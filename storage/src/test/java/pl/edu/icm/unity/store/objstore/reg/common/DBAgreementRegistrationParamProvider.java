/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.types.common.DBI18nString;

public class DBAgreementRegistrationParamProvider
{

	public static DBAgreementRegistrationParam getRegistrationParam()
	{
		return DBAgreementRegistrationParam.builder()
				.withMandatory(false)
				.withText(DBI18nString.builder()
						.withDefaultValue("a")
						.build())
				.build();
	}
}