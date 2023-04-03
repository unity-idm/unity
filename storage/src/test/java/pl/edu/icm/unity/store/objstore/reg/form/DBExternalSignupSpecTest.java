/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBExternalSignupSpecTest extends DBTypeTestBase<DBExternalSignupSpec>
{

	@Override
	protected String getJson()
	{
		return "{\"specs\":[{\"authenticatorKey\":\"key\",\"optionKey\":\"option\"}]}\n";
	}

	@Override
	protected DBExternalSignupSpec getObject()
	{
		return DBExternalSignupSpec.builder()
				.withSpecs(List.of(DBAuthenticationOptionsSelector.builder().withAuthenticatorKey("key")
						.withOptionKey("option").build()))
				.build();
	}

}
