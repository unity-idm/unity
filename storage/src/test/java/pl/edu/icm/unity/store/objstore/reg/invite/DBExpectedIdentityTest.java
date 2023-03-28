/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBExpectedIdentityTest extends DBTypeTestBase<DBExpectedIdentity>
{

	@Override
	protected String getJson()
	{
		return "{\"identity\":\"identity\",\"expectation\":\"MANDATORY\"}\n";
	}

	@Override
	protected DBExpectedIdentity getObject()
	{
		return DBExpectedIdentity.builder()
				.withExpectation("MANDATORY")
				.withIdentity("identity")
				.build();
	}

}
