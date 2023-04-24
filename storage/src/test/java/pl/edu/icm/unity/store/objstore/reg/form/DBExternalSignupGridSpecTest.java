/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;


public class DBExternalSignupGridSpecTest extends DBTypeTestBase<DBExternalSignupGridSpec>
{

	@Override
	protected String getJson()
	{
		return "{\"specs\":[{\"authenticatorKey\":\"key\",\"optionKey\":\"option\"}],"
				+ "\"gridSettings\":{\"searchable\":true,\"height\":1}}\n";
	}

	@Override
	protected DBExternalSignupGridSpec getObject()
	{
		return DBExternalSignupGridSpec.builder()
				.withSpecs(List.of(DBAuthenticationOptionsSelector.builder()
						.withAuthenticatorKey("key")
						.withOptionKey("option")
						.build()))
				.withGridSettings(DBAuthnGridSettings.builder()
						.withHeight(1)
						.withSearchable(true)
						.build())
				.build();
	}

}
