/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.messages;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBMessageTest extends DBTypeTestBase<DBMessage>
{
	@Override
	protected String getJson()
	{
		return "{\"name\":\"name\",\"locale\":\"pl\",\"value\":\"value\"}\n";
	}

	@Override
	protected DBMessage getObject()
	{
		return DBMessage.builder()
				.withLocale("pl")
				.withName("name")
				.withValue("value")
				.build();
	}

}
