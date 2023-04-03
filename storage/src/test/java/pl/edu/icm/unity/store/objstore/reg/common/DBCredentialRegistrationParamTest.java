/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBCredentialRegistrationParamTest extends DBTypeTestBase<DBCredentialRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"credentialName\":\"name\",\"label\":\"label\",\"description\":\"desc\"}\n";
	}

	@Override
	protected DBCredentialRegistrationParam getObject()
	{
		return DBCredentialRegistrationParam.builder()
				.withCredentialName("name")
				.withDescription("desc")
				.withLabel("label")
				.build();
	}
}
