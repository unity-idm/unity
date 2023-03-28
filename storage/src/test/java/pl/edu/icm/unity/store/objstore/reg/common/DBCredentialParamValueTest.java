/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBCredentialParamValueTest extends DBTypeTestBase<DBCredentialParamValue>
{

	@Override
	protected String getJson()
	{
		return "{\"credentialId\":\"credential\",\"secrets\":\"secret\"}\n";
	}

	@Override
	protected DBCredentialParamValue getObject()
	{
		return DBCredentialParamValue.builder()
				.withCredentialId("credential")
				.withSecrets("secret")
				.build();
	}

}
