/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identitytype;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBEmailConfirmationConfigurationTest extends DBTypeTestBase<DBEmailConfirmationConfiguration>
{

	@Override
	protected String getJson()
	{
		return "{\"messageTemplate\":\"template\",\"validityTime\":10}\n";
	}

	@Override
	protected DBEmailConfirmationConfiguration getObject()
	{
		return DBEmailConfirmationConfiguration.builder()
				.withValidityTime(10)
				.withMessageTemplate("template")
				.build();
	}

}
