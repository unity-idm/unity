/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBGroupPropertyTest extends DBTypeTestBase<DBGroupProperty>
{

	@Override
	protected String getJson()
	{
		return "{\"key\":\"key\",\"value\":\"val\"}";

	}

	@Override
	protected DBGroupProperty getObject()
	{
		return DBGroupProperty.builder()
				.withKey("key")
				.withValue("val")
				.build();
	}

}
